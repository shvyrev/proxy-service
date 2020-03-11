package org.acme;

import com.maxmind.geoip2.DatabaseReader;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.axle.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.acme.model.Proxy;
import org.acme.model.ProxyType;
import org.acme.services.AzureS3Service;
import org.acme.services.Zip;
import org.acme.utils.Utils;
import org.acme.workers.FileWorker;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.acme.model.ProxyType.*;

@ApplicationScoped
public class ProxyParser {

    private static final Logger log = LoggerFactory.getLogger( ProxyParser.class );
    private static final String TXT_EXTENSION = "txt";
    private static final String ZIP_EXTENSION = "zip";
    private static final String CSV_EXTENSION = "csv";
    private static final String JSON_EXTENSION = "json";
    public static final String FILE_NAME = "proxies";

    @Inject
    Vertx vertx;

    @Inject
    AzureS3Service azureS3Manager;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    FileWorker file;

    @Inject
    Cache cache;

    @ConfigProperty(name = "azure.storage.city.base")
    String cityBaseZip;

    Path cityBase = Paths.get("GeoLite2-City.mmdb");

    @Inject
    Zip zip;

    public static final String TMP_FILE_PATTERN = "tmp/%s.%s";
    public static final String RESULT_FILE_PATTERN = "results/%s.%s";

    private DatabaseReader geoReader;

    @ConsumeEvent(value = "proxy-ready")
    public void onProxyReady(String value){
        log.info(" $ onProxyReady : " + cache.size(Proxy.class));

        cleanUpFolders()
                .thenCompose(aVoid -> enrichData())
                .thenCompose(this::txtFiles)
                .thenCompose(this::saveCsv)
                .thenCompose(this::saveJson)
                .thenCompose(this::zip)
                .thenApply(this::report)
                .thenCompose(this::upload)
                .exceptionally(Utils::throwableHandler)
                .thenAccept(jsonObject -> log.info(" $ onProxyReady : " + jsonObject));
    }

    private CompletionStage<Void> cleanUpFolders() {
        return file.tryClean(Paths.get(TMP_FILE_PATTERN).getParent())
                .thenCompose(aVoid -> file.tryClean(Paths.get(RESULT_FILE_PATTERN).getParent()));
    }

    private CompletionStage<Void> upload(Void aVoid) {
        return azureS3Manager.uploadFiles(Paths.get(RESULT_FILE_PATTERN).getParent());
    }

    private Void report(Void aVoid) {
        final List<JsonObject> byCountry = cache.streamProxy().map(proxy -> proxy.country).distinct()
                .map(s -> new JsonObject().put("country", s).put("size", cache.size("country", s, Proxy.class)))
                .collect(Collectors.toList());

        final JsonObject jsonObject = new JsonObject()
                .put("amount", cache.size(Proxy.class))
                .put(Https.getValue(), cache.size("type", Https.getValue(), Proxy.class))
                .put(Socks4.getValue(), cache.size("type", Socks4.getValue(), Proxy.class))
                .put(Socks5.getValue(), cache.size("type", Socks5.getValue(), Proxy.class))
                .put("byCountry", new JsonArray(byCountry));
        try {
            Files.write(
                    Paths.get(RESULT_FILE_PATTERN).getParent().resolve("report.json")
                    , jsonObject.encode().getBytes(StandardCharsets.UTF_8)
                    , StandardOpenOption.CREATE
            );
        } catch (Exception ie) {}

        return null;
    }

    private CompletionStage<Void> zip(Void v) {
        final Path tmpFolder = Paths.get(format(TMP_FILE_PATTERN, "", "")).getParent();
        return zipFiles(TXT_EXTENSION, tmpFolder)
                .thenCompose(p -> zipFiles(CSV_EXTENSION, tmpFolder))
                .thenCompose(p -> zipFiles(JSON_EXTENSION, tmpFolder))
                .thenCompose(p -> zipFiles(null, tmpFolder))
                .thenApply(path -> null);
    }

    private CompletionStage<Path> zipFiles(String ext, Path folder) {
        return file.createFile(format(RESULT_FILE_PATTERN, ext != null ? join("_", FILE_NAME, ext) : FILE_NAME, ZIP_EXTENSION))
                .thenCompose(p -> zip.files(p, ext != null ? file.filesByExt(ext, folder) : file.files(folder)));
    }

    private CompletionStage<Void> txtFiles(Void aVoid) {
        return saveTxt(Https)
                .thenCompose(a -> saveTxt(Socks4))
                .thenCompose(a -> saveTxt(Socks5));
    }

    private CompletionStage<Void> saveCsv(Void aVoid) {
        return file.createFile(format(TMP_FILE_PATTERN, FILE_NAME, CSV_EXTENSION))
                .thenCompose(path -> vertx.executeBlocking(event -> {
                    final String header = join(",", List.of("Host", "Port", "Type", "Country", "Country code", "City"));
                    try {
                        Files.write(path, (header + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                        cache.streamProxy().forEach(proxy -> {
                            try {
                                final String line = join(", ", proxy.host, proxy.port.toString(), proxy.type.getValue(), proxy.country, proxy.countryCode, proxy.city == null ? "" : proxy.city);
                                Files.write(path, (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                            } catch (Exception e) {}
                        });
                        event.complete();
                    } catch (IOException e) {
                        event.fail(e);
                    }
                }));
    }

    private CompletionStage<Void> saveJson(Void aVoid) {
        return file.createFile(format(TMP_FILE_PATTERN, FILE_NAME, JSON_EXTENSION))
                .thenCompose(path -> vertx.executeBlocking(event -> {
                    try{
                        Files.write(path, ("[\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

                        AtomicBoolean isFirst = new AtomicBoolean(true);
                        cache.streamProxy().forEach(proxy -> {
                            try {
                                Files.write(path, (proxy.toJsonString() + ",\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                                isFirst.set(false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        Files.write(path, ("{}]").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                        event.complete();
                    } catch (Exception e){
                        event.fail(e);
                    }
                }));
    }

    private CompletionStage<Void> saveTxt(ProxyType proxyType){
        return file.createFile(format(TMP_FILE_PATTERN, proxyType.getValue(), TXT_EXTENSION))
                .thenCompose(path -> vertx.executeBlocking(event -> {
                    cache.streamProxyByType(proxyType).forEach(proxy -> {
                        try {
                            Files.write(path, (proxy.host + ":" + proxy.port + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                        } catch (Exception ignored){}
                    });
                    event.complete();
                }));
    }

    private CompletionStage<Void> enrichData() {
        return uploadIfNotExistsCityBase()
                .thenCompose(aVoid -> vertx.executeBlocking(event -> {
                    cache.streamProxy().map(proxy -> {
                        try {
                            return proxy.geo(geoReader().city(proxy.ipAddress()));
                        } catch (Exception e) {
                            return proxy;
                        }
                    }).forEach(cache::upsert);
                    event.complete();
                }));
    }

    private CompletionStage<Void> uploadIfNotExistsCityBase() {
        final Path tmpZipFile = Paths.get("tmp" + UUID.randomUUID().toString() + ".zip");
        return !Files.exists(cityBase)
                ? azureS3Manager.download(tmpZipFile, cityBaseZip, null)
                    .thenCompose(aVoid -> zip.unzip(tmpZipFile, Paths.get("")))
                    .thenApply(path -> {
                        try {
                            Files.delete(tmpZipFile);
                        } catch (Exception e) {}
                        return null;
                    })
                : CompletableFuture.completedStage(null);
    }

    private DatabaseReader geoReader() throws Exception {
        return geoReader = new DatabaseReader.Builder(cityBase.toFile()).build();
    }

    private void runLater(Runnable runnable) {
        vertx.timerStream(250).handler(l -> managedExecutor.execute(runnable));
    }

}
