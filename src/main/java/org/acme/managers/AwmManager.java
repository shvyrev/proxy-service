package org.acme.managers;

import io.quarkus.scheduler.Scheduled;
import io.vertx.axle.core.Vertx;
import org.acme.Cache;
import org.acme.tasks.ContentTask;
import org.acme.tasks.LinkTask;
import org.acme.utils.Utils;
import org.acme.workers.ContentWorker;
import org.acme.workers.HTTPWorker;
import org.acme.workers.LinkWorker;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;

public class AwmManager extends ManagerImpl {

    public static final String TYPE = "awm-proxy";

    @ConfigProperty(name = "config.url")
    String configUrl;

    @Inject
    Cache cache;

    @Inject
    LinkWorker linkWorker;

    @Inject
    ContentWorker contentWorker;

    @Inject
    HTTPWorker http;

    @Inject
    Vertx vertx;

    @Scheduled(every = "1m")
    void onTime(){
        config(TYPE, configUrl, http)
                .thenApply(s -> cache.addIfNotExist(ContentTask.of(TYPE, s)))
                .exceptionally(Utils::throwableHandler)
                .thenAccept(aVoid -> next());
    }

    private void next() {
        task(TYPE)
                .map(task -> task instanceof LinkTask ? linkWorker.awmProxy((LinkTask) task) : contentWorker.awmProxy((ContentTask) task))
                .ifPresent(completionStage -> completionStage.thenCompose(this::save)
                        .exceptionally(Utils::throwableHandler).thenAccept(s -> next()));
    }


    @Override
    Vertx getVertx() {
        return vertx;
    }

    @Override
    Cache getCache() {
        return cache;
    }
}
