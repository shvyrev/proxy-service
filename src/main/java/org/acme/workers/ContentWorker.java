package org.acme.workers;

import org.acme.Cache;
import org.acme.model.Proxy;
import org.acme.model.ProxyType;
import org.acme.tasks.ContentTask;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.acme.Constants.IP_PORT_PATTERN;
import static org.acme.Constants.SCRIPT_DATA_PATTERN;
import static org.acme.model.ProxyType.*;

@ApplicationScoped
public class ContentWorker {

    @Inject
    Cache cache;

    @Inject
    HTTPWorker http;

    @Inject
    FileWorker file;

    private static final Logger log = LoggerFactory.getLogger( ContentWorker.class );

    public CompletionStage<List<Proxy>> awmProxy(ContentTask task){
        cache.remove(task);
        return http.getHtml(task.url)
                .exceptionally(th -> "")
                .thenApply(html -> getPatternStream(IP_PORT_PATTERN, html).map(Proxy::of).collect(toList()));
    }

    private Stream<String> getPatternStream(Pattern pattern, String html) {
        return pattern.matcher(html).results().map(MatchResult::group).distinct();
    }

    public CompletionStage<List<Proxy>> openProxy(ContentTask task) {
        cache.remove(task);
        return http.parse(task.getUrl())
                .thenApply(document -> {
                    return document.select("script").stream()
                            .map(element -> SCRIPT_DATA_PATTERN.matcher(element.toString()).results().map(MatchResult::group).collect(toList()))
                            .filter(not(List::isEmpty))
                            .map(Object::toString)
                            .map(s -> IP_PORT_PATTERN.matcher(s).results().map(MatchResult::group).collect(toList()))
                            .flatMap(Collection::stream)
                            .map(txt -> Proxy.of(txt, getOpenProxyProxyType(document)))
                            .collect(toList());
                });
    }

    private ProxyType getOpenProxyProxyType(Document document) {
        return document.select("div[class=pa] span").stream()
                .filter(element -> element.text().contains("socks"))
                .findFirst().map(element -> element.text().toLowerCase().contains("socks4") ? Socks4 : Socks5)
                .orElse(Https);
    }
}
