package org.acme.managers;

import io.vertx.axle.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.acme.Cache;
import org.acme.tasks.ContentTask;
import org.acme.tasks.LinkTask;
import org.acme.tasks.TaskImpl;
import org.acme.utils.IdImpl;
import org.acme.workers.HTTPWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public abstract class ManagerImpl {

    private static final Logger log = LoggerFactory.getLogger( ManagerImpl.class );

    <U> CompletionStage<U> save(List<? extends IdImpl> list) {
        return getVertx().executeBlocking(event -> {
            list.forEach(getCache()::addIfNotExist);
            event.complete();
        });
    }

    <V extends TaskImpl> Optional<V> task(String type) {
        final long contentTaskAmount = getCache().size("type", type, ContentTask.class);
        final long linkTaskAmount = getCache().size("type", type, LinkTask.class);
        final Class aClass = contentTaskAmount > linkTaskAmount ? ContentTask.class : LinkTask.class;

        if (contentTaskAmount != 0 || linkTaskAmount != 0) {
            return getCache().first("type", type, aClass);
        } else {
            return Optional.empty();
        }
    }

    CompletionStage<String> config(String type, String configUrl, HTTPWorker httpWorker) {
        return httpWorker.jsonArray(configUrl)
                .thenApply(jsonArray -> {
                    final Optional<JsonObject> result = jsonArray.stream().map(o -> (JsonObject) o)
                            .filter(json -> json.getString("type", "").equalsIgnoreCase(type)).findFirst();
                    return result.isPresent() ? result.get().getJsonObject("data", new JsonObject()).getString("url", "") : "";
                });
    }


    abstract Vertx getVertx();
    abstract Cache getCache();
}
