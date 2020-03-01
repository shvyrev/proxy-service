package org.acme.managers;

import io.quarkus.scheduler.Scheduled;
import io.vertx.axle.core.Vertx;
import org.acme.Cache;
import org.acme.model.Proxy;
import org.acme.tasks.ContentTask;
import org.acme.tasks.LinkTask;
import org.acme.utils.Utils;
import org.acme.workers.ContentWorker;
import org.acme.workers.HTTPWorker;
import org.acme.workers.LinkWorker;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.acme.utils.Utils.nowMillis;

public class OpenProxyManager extends ManagerImpl{

    public static final String TYPE = "open-proxy-space";

    @ConfigProperty(name = "config.url")
    String configUrl;

    @Inject
    Vertx vertx;

    @Inject
    Cache cache;

    @Inject
    HTTPWorker http;

    @Inject
    LinkWorker linkWorker;

    @Inject
    ContentWorker contentWorker;

    private int itemPerPage = 18;
    private int depth = 3;

    @Scheduled(every = "6h")
    void onTime(){
        config(TYPE, configUrl, http)
                .thenApply(s ->
                        IntStream.range(0, depth)
                                .mapToObj(i -> cache.addIfNotExist(LinkTask.of(TYPE, format(s, i * itemPerPage, nowMillis()))))
                                .collect(toList())
                )
                .exceptionally(Utils::throwableHandler)
                .thenAccept(aVoid -> next());
    }

    private void next() {
        task(TYPE)
                .map(task -> task instanceof LinkTask ? linkWorker.openProxy((LinkTask) task) : contentWorker.openProxy((ContentTask) task))
                .ifPresent(compStage -> compStage.thenCompose(this::save)
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
