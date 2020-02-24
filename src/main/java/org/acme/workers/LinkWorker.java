package org.acme.workers;

import org.acme.Cache;
import org.acme.tasks.LinkTask;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class LinkWorker {

    @Inject
    Cache cache;

    public CompletionStage<List<LinkTask>> awmProxy(LinkTask task){
        cache.remove(task);
        return CompletableFuture.completedStage(List.of());
    }
}
