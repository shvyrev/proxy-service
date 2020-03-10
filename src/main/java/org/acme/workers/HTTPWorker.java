package org.acme.workers;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.file.AsyncFile;
import io.vertx.axle.core.file.FileSystem;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.axle.ext.web.codec.BodyCodec;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClientOptions;
import org.acme.model.Proxy;
import org.acme.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static org.acme.Constants.*;
import static org.acme.ProxyChecker.CONNECT_TIMEOUT;
import static org.acme.utils.Utils.rndUUID;

@ApplicationScoped
public class HTTPWorker {

    private static final Logger log = LoggerFactory.getLogger( HTTPWorker.class );

    @Inject
    Vertx vertx;

    public CompletionStage<String> getHtml(String url){
        final WebClient client = getClient();
        return client.getAbs(url)
                .send()
                .exceptionally(Utils::throwableHandler)
                .thenCompose(resp -> {
                    client.close();
                    return CompletableFuture.supplyAsync(() -> resp.bodyAsString(StandardCharsets.UTF_8.name()));
                });
    }

    public CompletionStage<JsonArray> jsonArray(String url){
        final WebClient client = getClient();
        return client.getAbs(url)
                .send()
                .exceptionally(Utils::throwableHandler)
                .thenCompose(resp -> {
                    client.close();
                    return CompletableFuture.supplyAsync(() -> new JsonArray(resp.bodyAsString(StandardCharsets.UTF_8.name())));
                });
    }

    public CompletionStage<Document> parse(String url){
        return getHtml(url).thenCompose(s -> CompletableFuture.completedFuture(Jsoup.parse(s)));
    }

    public CompletionStage<JsonObject> geo(String ip){
        final WebClient client = getClient();
        return client.getAbs(format("https://freegeoip.app/json/%s", ip)).send()
                .exceptionally(Utils::throwableHandler)
                .thenApply(response -> {
                    client.close();
                    return response != null && response.statusCode() == 200 ? response.bodyAsJsonObject() : new JsonObject();
                });
    }

    public CompletionStage<String> download(String url){
        final WebClient client = getClient();
        final FileSystem fileSystem = vertx.fileSystem();
        return fileSystem.createTempFile(rndUUID(), "")
                .thenCompose(s -> {
                    final AsyncFile asyncFile = fileSystem.openBlocking(s, new OpenOptions().setSync(true));
                    return client.getAbs(url).as(BodyCodec.pipe(asyncFile)).send()
                            .thenCompose(voidHttpResponse -> {
                                client.close();
                                return CompletableFuture.completedFuture(s);
                            });
                });
    }

    protected WebClient getClient(){
        return WebClient.create(vertx, getOptions());
    }

    @SuppressWarnings("UnstableApiUsage")
    private WebClientOptions getOptions() {

        final WebClientOptions clientOptions = new WebClientOptions()
                .setSsl(true)
                .setLogActivity(true)
                .setDefaultPort(SSL_PORT)
                .setKeepAlive(true)
                .setConnectTimeout(TIMEOUT)
                .setUserAgent(USER_AGENT)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setFollowRedirects(true);
        return clientOptions;
    }

    public WebClient getClient(String host, int port, Proxy proxy) {
        final WebClientOptions options = new WebClientOptions()
                .setDefaultHost(host)
                .setDefaultPort(port)
                .setFollowRedirects(true)
                .setTrustAll(true)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSsl(true);
        if (proxy != null) {
            options.setProxyOptions(createProxyOption(proxy));
        }
        return WebClient.create(vertx, options);
    }

    private ProxyOptions createProxyOption(Proxy proxy) {
        return new ProxyOptions().setHost(proxy.host).setPort(proxy.port).setType(ProxyType.HTTP);
    }


}
