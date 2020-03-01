package org.acme.workers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.acme.Cache;
import org.acme.tasks.ContentTask;
import org.acme.tasks.LinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class LinkWorker {

    @Inject
    Cache cache;

    @Inject
    HTTPWorker http;

    public static final String OPENPROXY_SPACE_PATTERN = "https://openproxy.space/list/%s";

    private static final Logger log = LoggerFactory.getLogger( LinkWorker.class );

    public CompletionStage<List<ContentTask>> awmProxy(LinkTask task){
        log.info(" $ awmProxy : " + task);
        cache.remove(task);
        return CompletableFuture.completedStage(List.of());
    }

    public CompletionStage<List<ContentTask>> openProxy(LinkTask task) {
        cache.remove(task);
        return http.getHtml(task.getUrl())
                .thenApply(JsonArray::new)
                .thenApply(jsonArray -> jsonArray.stream()
                        .map(JsonObject::mapFrom)
                        .map(jsonObject -> ContentTask.of(task.type,
                                format(OPENPROXY_SPACE_PATTERN, jsonObject.getString("code", ""))))
                        .collect(toList())
                );
    }
}
