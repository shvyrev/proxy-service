package org.acme.managers;

import io.quarkus.scheduler.Scheduled;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.eventbus.EventBus;
import org.acme.Cache;
import org.acme.tasks.ContentTask;
import org.acme.tasks.LinkTask;
import org.acme.utils.Utils;
import org.acme.workers.ContentWorker;
import org.acme.workers.HTTPWorker;
import org.acme.workers.LinkWorker;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
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

    @Inject
    ManagedExecutor managedExecutor;

    private int itemPerPage = 18;
    private int depth = 2;

    @Inject
    EventBus eventBus;

    @Scheduled(every = "20m")
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
                .ifPresentOrElse(compStage -> compStage.thenCompose(this::save)
                        .exceptionally(Utils::throwableHandler).thenAccept(s -> runLater(this::next)),
                        () -> eventBus.publish("proxy-ready", null));
    }

    @Override
    Vertx getVertx() {
        return vertx;
    }

    @Override
    Cache getCache() {
        return cache;
    }

    private void runLater(Runnable runnable) {
        vertx.timerStream(250).handler(l -> managedExecutor.execute(runnable));
    }

}
