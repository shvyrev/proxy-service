package org.acme.services;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.microsoft.azure.storage.file.FileProperties;
import io.vertx.axle.core.Promise;
import io.vertx.axle.core.Vertx;
import io.vertx.core.Handler;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class AzureS3Service {

    @ConfigProperty(name = "azure.storage.connection.string")
    String azureStorageConnectionString;

    @ConfigProperty(name = "azure.storage.name")
    String azureStorageName;

    @Inject
    Vertx vertx;

    private CloudFileShare azureStore;

    public CompletionStage<Boolean> mkDir(String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(mkDir(dirName, store)));
    }

    public CompletionStage<Boolean> rmDir(String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(tmDir(dirName, store)));
    }

    public CompletionStage<List<String>> list(){
        return store().thenCompose(store -> vertx.executeBlocking(list(null, store)));
    }

    public CompletionStage<List<String>> list(String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(list(dirName, store)));
    }

    public CompletionStage<Void> upload(Path filePath){
        return store().thenCompose(store -> vertx.executeBlocking(upload(filePath, null, store)));
    }

    public CompletionStage<Void> upload(Path filePath, String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(upload(filePath, dirName, store)));
    }

    public CompletionStage<Void> uploadFiles(Path folder) {
        try {
            return CompletableFuture.allOf(
                    Files.walk(folder).filter(not(Files::isDirectory))
                            .map(this::upload)
                            .toArray(CompletableFuture[]::new)
            );
        } catch (Exception ex) {
            return CompletableFuture.failedStage(ex);
        }

//        return store().thenCompose(store -> vertx.executeBlocking(uploadFiles(folder, store)));
    }

    public CompletionStage<FileProperties> properties(String fileName){
        return store().thenCompose(store -> vertx.executeBlocking(properties(fileName, null, store)));
    }

    public CompletionStage<FileProperties> properties(String fileName, String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(properties(fileName, dirName, store)));
    }

    public CompletionStage<Map<String, String>> meta(String fileName){
        return store().thenCompose(store -> vertx.executeBlocking(meta(fileName, null, store)));
    }

    public CompletionStage<Map<String, String>> meta(String fileName, String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(meta(fileName, dirName, store)));
    }

    public CompletionStage<Void> download(Path destFilePath, String fileName, String dirName){
        return store().thenCompose(store -> vertx.executeBlocking(download(destFilePath, fileName, dirName, store)));
    }

    private Handler<Promise<Boolean>> delete(Path path, String fileName, String dirName, CloudFileShare store){
        return event -> {
            try {
                event.complete(getCloudFileDirectory(dirName, store).getFileReference(fileName).deleteIfExists());
            } catch (Exception ex) {
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<Void>> download(Path destFilePath, String fileName, String dirName, CloudFileShare store){
        return event -> {
            try {
                getCloudFileDirectory(dirName, store).getFileReference(fileName)
                        .download(Files.newOutputStream(destFilePath, StandardOpenOption.CREATE));
                event.complete();
            } catch (Exception ex) {
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<Void>> upload(Path filePath, String dirName, CloudFileShare store){
        return event -> {
            try {
                getCloudFileDirectory(dirName, store).getFileReference(filePath.getFileName().toString())
                        .upload(Files.newInputStream(filePath), Files.size(filePath));
                event.complete();
            } catch (Exception ex) {
                ex.printStackTrace();
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<FileProperties>> properties(String fileName, String dirName, CloudFileShare store){
        return event -> {
            try {
                final CloudFile fileReference = getCloudFileDirectory(dirName, store).getFileReference(fileName);
                if(fileReference.exists()){
                    event.complete(fileReference.getProperties());
                } else {
                    event.fail(new FileNotFoundException(fileName));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<Map<String , String>>> meta(String fileName, String dirName, CloudFileShare store){
        return event -> {
            try {
                final CloudFile fileReference = getCloudFileDirectory(dirName, store).getFileReference(fileName);
                if(fileReference.exists()){
                    event.complete(fileReference.getMetadata());
                } else {
                    event.fail(new FileNotFoundException(fileName));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<List<String>>> list(String dirName, CloudFileShare store) {
        return event -> {
            try {
                event.complete(
                        StreamSupport.stream(getCloudFileDirectory(dirName, store)
                                .listFilesAndDirectories().spliterator(),false)
                                .map(listFileItem -> fileName(listFileItem.getUri())).collect(toList())
                );
            } catch (Exception ex) {
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<Boolean>> mkDir(String dirName, CloudFileShare store){
        return event -> {
            try {
                event.complete(store.getRootDirectoryReference().getDirectoryReference(dirName).createIfNotExists());
            } catch (Exception ex) {
                event.fail(ex);
            }
        };
    }

    private Handler<Promise<Boolean>> tmDir(String dirName, CloudFileShare store) {
        return event -> {
            try {
                event.complete(store.getRootDirectoryReference().getDirectoryReference(dirName).deleteIfExists());
            } catch (Exception ex) {
                event.fail(ex);
            }
        };
    }

    private List<String> getCollect(String dirName, CloudFileShare store) throws URISyntaxException, StorageException {
        return StreamSupport.stream(store.getRootDirectoryReference().getDirectoryReference(dirName)
                .listFilesAndDirectories().spliterator(),false)
                .map(listFileItem -> fileName(listFileItem.getUri())).collect(toList());
    }

    private CompletionStage<CloudFileShare> store(){
        return azureStore == null ? vertx.executeBlocking(event -> {
            try {
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(azureStorageConnectionString);
                azureStore = storageAccount.createCloudFileClient().getShareReference(azureStorageName);
                azureStore.createIfNotExists();
                event.complete(azureStore);
            } catch (Exception ex) {
                event.fail(ex);
            }
        }) : CompletableFuture.completedStage(azureStore);
    }

    private String fileName(URI uri){
        return (new File(uri.getPath())).getName();
    }

    private CloudFileDirectory getCloudFileDirectory(String dirName, CloudFileShare store) throws StorageException, URISyntaxException {
        final CloudFileDirectory cloudDirectory = dirName == null
                ? store.getRootDirectoryReference()
                : store.getRootDirectoryReference().getDirectoryReference(dirName);
        cloudDirectory.createIfNotExists();
        return cloudDirectory;
    }
}
