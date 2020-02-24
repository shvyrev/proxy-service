package org.acme.managers;

import io.quarkus.scheduler.Scheduled;
import io.vertx.axle.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.acme.Cache;
import org.acme.tasks.ContentTask;
import org.acme.tasks.LinkTask;
import org.acme.tasks.TaskImpl;
import org.acme.utils.IdImpl;
import org.acme.utils.Utils;
import org.acme.workers.ContentWorker;
import org.acme.workers.HTTPWorker;
import org.acme.workers.LinkWorker;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class AwmManager {

    public static final String TYPE = "awm-proxy";
    private static final Logger log = LoggerFactory.getLogger( AwmManager.class );

    @ConfigProperty(name = "config.url")
    String configUrl;

    private String url = "https://awmproxy.com/freeproxy_667c01a0cf31efe.txt";

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

    @Scheduled(every = "1h")
    void onTime(){
        updateConfig().thenApply(s -> cache.addIfNotExist(ContentTask.of(TYPE, url)))
                .exceptionally(Utils::throwableHandler)
                .thenAccept(aVoid -> next());
    }

    private CompletionStage<String> updateConfig() {
        return http.jsonArray(configUrl)
                .thenApply(jsonArray -> {
                    jsonArray.stream().map(o -> (JsonObject)o)
                            .filter(json -> json.getString("type", "").equalsIgnoreCase(TYPE)).findFirst()
                            .ifPresent(json -> url = json.getJsonObject("data", new JsonObject()).getString("url", ""));
                    return url;
                });
    }

    private void next() {
        getTask()
                .map(task -> task instanceof LinkTask ? linkWorker.awmProxy((LinkTask) task) : contentWorker.awmProxy((ContentTask) task))
                .ifPresent(completionStage -> completionStage.thenCompose(this::save)
                        .exceptionally(Utils::throwableHandler).thenAccept(s -> next()));
    }

    private <U> CompletionStage<U> save(List<? extends IdImpl> list) {
        return vertx.executeBlocking(event -> {
            list.forEach(cache::addIfNotExist);
            event.complete();
        });
    }

    private <V extends TaskImpl> Optional<V> getTask() {
        final long contentTaskAmount = cache.size("type", TYPE, ContentTask.class);
        final long linkTaskAmount = cache.size("type", TYPE, LinkTask.class);
        final Class aClass = contentTaskAmount > linkTaskAmount ? ContentTask.class : LinkTask.class;

        if (contentTaskAmount != 0 || linkTaskAmount != 0) {
            return cache.first("type", TYPE, aClass);
        } else {
            return Optional.empty();
        }
    }

}
