package org.acme.workers;

import io.vertx.axle.core.Promise;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.core.file.FileSystem;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import static org.acme.utils.Utils.rndUUID;

@ApplicationScoped
public class FileWorker {

    @Inject
    Vertx vertx;

    public Path[] files(Path folder) {
        try {
            return Files.walk(folder).toArray(Path[]::new);
        } catch (IOException e) {
            return new Path[0];
        }
    }

    public CompletionStage<String> saveTo(String txt, String filePath) {
        final FileSystem fileSystem = vertx.fileSystem();

        return createFile(filePath)
                .thenCompose(aVoid -> fileSystem.writeFile(filePath, Buffer.buffer(txt)))
                .thenCompose(aVoid -> CompletableFuture.completedFuture(filePath));
    }

    public CompletionStage<Void> tryClean(Path folder){
        return mkDirIfNotExists(folder).thenCompose(path -> vertx.executeBlocking(clean(path)));
    }

    public Path[] filesByExt(String ext, Path folder){
        try {
            return Files.walk(folder).filter(path -> {
                final String s = path.getFileName().toString();
                return s.lastIndexOf(ext) == s.length() - ext.length();
            }).toArray(Path[]::new);
        } catch (IOException e) {
            return new Path[0];
        }
    }

    private Handler<Promise<Void>> clean(Path folder) {
        return event -> {
            try {
                Files.walk(folder).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {}
                });
                event.complete();
            } catch (IOException e) {
                event.fail(e);
            }
        };
    }

    public CompletionStage<Path> createFileForce(Path path){
        return mkDirIfNotExists(path.getParent())
                .thenCompose(p -> vertx.executeBlocking(forceCreateFile(path)));
    }

    private Handler<Promise<Path>> forceCreateFile(Path path) {
        return event -> {
            try {
                if (Files.exists(path)) {
                    Files.delete(path);
                }
                event.complete(Files.createFile(path));
            } catch (IOException e) {
                event.fail(e);
            }
        };
    }

    public CompletionStage<Path> mkDirIfNotExists(@Nullable Path path){
        if (path == null) {
            CompletableFuture.completedStage(null);
        }
        try {
            return CompletableFuture.completedStage(!Files.exists(path) ? Files.createDirectories(path) : path);
        } catch (IOException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private static final Logger log = LoggerFactory.getLogger( FileWorker.class );

    public CompletionStage<Path> createFile(String filePath){
        final Path result = Paths.get(filePath);
        final Path parent = result.getParent();
        return mkDirIfNotExists(parent)
                .thenCompose(path -> vertx.executeBlocking(createFile(result)));
    }

    private Handler<Promise<Path>> createFile(Path path) {
        return event -> {
            try {
                event.complete(Files.createFile(path));
            } catch (IOException e) {
                event.fail(e);
            }
        };
    }

    public CompletionStage<String> saveTo(String txt) {
        final FileSystem fileSystem = vertx.fileSystem();

        return fileSystem.createTempFile(rndUUID(), "")
                .thenCompose(filePath -> fileSystem.writeFile(filePath, Buffer.buffer(txt))
                        .thenCompose(aVoid -> CompletableFuture.completedFuture(filePath)));
    }

    public CompletionStage<String> read(String filePath){
        return vertx.fileSystem().readFile(filePath)
                .thenCompose(buffer -> CompletableFuture.completedFuture(buffer.toString(StandardCharsets.UTF_8)));
    }

    public CompletionStage<Document> parse(String filePath){
        return read(filePath).thenApply(Jsoup::parse);
    }

    public CompletionStage<JsonObject> readJson(String filePath){
        return vertx.fileSystem().readFile(filePath)
                .thenCompose(buffer -> CompletableFuture.completedFuture((JsonObject) buffer.toJson()));
    }

    public CompletionStage<JsonArray> readJsonArray(String filePath){
        return vertx.fileSystem().readFile(filePath)
                .thenCompose(buffer -> CompletableFuture.completedFuture(buffer.toJsonArray()));
    }

    public CompletionStage<Boolean> fileExist(String filePath){
        return vertx.fileSystem().exists(filePath);
    }

}
