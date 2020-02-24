package org.acme;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.ext.web.client.HttpResponse;
import org.acme.model.Proxy;
import org.acme.utils.Utils;
import org.acme.workers.HTTPWorker;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.acme.utils.Utils.nowMillis;

@ApplicationScoped
public class ProxyChecker {

    public static final int CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    public static final int TEST_PORT = 443;

    public static final List<String > hosts = List.of("api.ipify.org", "icanhazip.com");

    @Inject
    Cache cache;

    @Inject
    Vertx vertx;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    HTTPWorker http;

    private static final Logger log = LoggerFactory.getLogger( ProxyChecker.class );

    @Scheduled(every = "24h")
    void onTime(){
        IntStream.range(0, 10).forEach(i -> check());
    }

//    void onStart(@Observes @Priority(1)StartupEvent event){
//        IntStream.range(0, 10).forEach(i -> check());
//    }

    private void check() {
        cache.randomProxy().ifPresentOrElse(this::check, () -> runLater(this::check));
    }

    private void check(Proxy proxy) {
        final long startTime = nowMillis();
        request(proxy)
                .exceptionally(Utils::throwableHandler)
                .thenAccept(response -> {
                    if (response != null && response.statusCode() == 200) {
                        proxy.updatedAt = nowMillis();
                        proxy.latency = proxy.updatedAt - startTime;
                        cache.upsert(proxy);
                    }
                    runLater(this::check);
                });
    }

    private CompletionStage<HttpResponse<Buffer>> request(Proxy proxy) {
        return http.getClient(hosts.get(ThreadLocalRandom.current().nextInt(hosts.size())), TEST_PORT, proxy).get("").send();
    }

    private void runLater(Runnable runnable) {
        vertx.timerStream(250).handler(l -> managedExecutor.execute(runnable));
    }
}
