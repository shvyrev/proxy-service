package org.acme.services;

import io.vertx.axle.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class Zip {

    @Inject
    Vertx vertx;

    public CompletionStage<Path> unzip(Path sourceFile, Path destDir){
        return vertx.executeBlocking(event -> {
            try(ZipFile file = new ZipFile(sourceFile.toFile())) {
                final Enumeration<? extends ZipEntry> entries = file.entries();
                final Path destination = !Files.exists(destDir) ? Files.createDirectory(destDir) : destDir;

                while (entries.hasMoreElements()){
                    final ZipEntry entry = entries.nextElement();
                    final Path path = destination.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(path);
                    } else {
                        try(final InputStream inputStream = file.getInputStream(entry)){
                            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e){}
                    }
                }
                event.complete(destination);
            } catch (Exception ex) {
                event.fail(ex);
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger( Zip.class );

    public CompletionStage<Path> files(Path toFile, Path ... paths){
        return vertx.executeBlocking(event -> {
            try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(toFile)))){
                Stream.of(paths).forEach(p -> {
                    if (p.toFile().isFile()) {
                        try {
                            zip.putNextEntry(new ZipEntry(p.getFileName().toString()));
                            zip.write(Files.readAllBytes(p));
                            zip.closeEntry();
                        } catch (IOException e) {}
                    }
                });
                event.complete(toFile);
            }catch (Exception e){
                event.fail(e);
            }
        });
    }

    public CompletionStage<Path> folder(Path fromDir, Path toFile){
        return vertx.executeBlocking(event -> {
            try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(toFile)))){
                Files.walk(fromDir).forEach(p -> {
                    if (p.toFile().isFile()) {
                        try {
                            zip.putNextEntry(new ZipEntry(fromDir.relativize(p).toString()));
                            zip.write(Files.readAllBytes(p));
                            zip.closeEntry();
                        } catch (IOException e) {}
                    }
                });
                event.complete(toFile);
            }catch (Exception e){
                event.fail(e);
            }
        });
    }

    public CompletionStage<Path> file(Path filePath, Path toFile){
         return vertx.executeBlocking(event -> {
             try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(toFile)))){
                 zip.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
                 zip.write(Files.readAllBytes(filePath));
                 zip.closeEntry();
                 event.complete(toFile);
             }catch (Exception e){
                 event.fail(e);
             }
         });
    }

}
