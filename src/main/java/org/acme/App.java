package org.acme;

import io.quarkus.runtime.StartupEvent;
import org.acme.utils.Utils;
import org.acme.workers.FileWorker;
import org.acme.workers.HTTPWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;

import static org.acme.ProxyParser.RESULT_FILE_PATTERN;
import static org.acme.ProxyParser.TMP_FILE_PATTERN;

@ApplicationScoped
public class App {
    private static final Logger log = LoggerFactory.getLogger( App.class );

    @Inject
    HTTPWorker http;

    @Inject
    FileWorker file;

    void onStart(@Observes @Priority(1)StartupEvent event){
        log.info(" $ onStart : " + event);

//        cleanUpFolders()
//                .exceptionally(Utils::throwableHandler)
//                .thenAccept(aVoid -> log.info(" $ onStart : " + "cleaned"));

    }

}
