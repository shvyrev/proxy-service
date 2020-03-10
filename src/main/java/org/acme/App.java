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

@ApplicationScoped
public class App {
    private static final Logger log = LoggerFactory.getLogger( App.class );

    @Inject
    HTTPWorker http;

    @Inject
    FileWorker file;

    void onStart(@Observes @Priority(1)StartupEvent event){
        log.info(" $ onStart : " + event);

    }
}
