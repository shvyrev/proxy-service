package org.acme.workers;

import org.acme.Cache;
import org.acme.model.Proxy;
import org.acme.tasks.ContentTask;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static org.acme.Constants.IP_PORT_PATTERN;

@ApplicationScoped
public class ContentWorker {

    @Inject
    Cache cache;

    @Inject
    HTTPWorker http;

    public CompletionStage<List<Proxy>> awmProxy(ContentTask task){
        cache.remove(task);
        return http.getHtml(task.url)
                .exceptionally(th -> "")
                .thenApply(html -> getPatternStream(IP_PORT_PATTERN, html)
                        .map(s -> Proxy.of(s.split(":")[0], parseInt(s.split(":")[1]))).collect(toList()));
    }

    private Stream<String> getPatternStream(Pattern pattern, String html) {
        return pattern.matcher(html).results().map(MatchResult::group).distinct();
    }
}
