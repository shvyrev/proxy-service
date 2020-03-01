package org.acme;

import io.quarkus.scheduler.Scheduled;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.ext.web.client.HttpResponse;
import org.acme.model.Proxy;
import org.acme.model.Report;
import org.acme.model.ReportEntity;
import org.acme.utils.Utils;
import org.acme.workers.HTTPWorker;
import org.eclipse.microprofile.context.ManagedExecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.acme.utils.Utils.nowMillis;

@ApplicationScoped
public class ProxyChecker {

    public static final int CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(20);
    public static final int TEST_PORT = 443;

    public static final List<String > hosts = List.of("ya.ru");

    @Inject
    Cache cache;

    @Inject
    Vertx vertx;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    HTTPWorker http;

    @Transactional
    @Scheduled(every = "6h")
    void onTime(){
        cache.lastReport().ifPresent(this::saveReport);
        cache.upsert(Report.create());
        IntStream.range(0, Runtime.getRuntime().availableProcessors() >> 2).forEach(i -> check());
    }

    private void saveReport(Report report) {
        ReportEntity.of(
                report.finish(cache.checkedProxyAmount(), cache.availableProxyAmount(),
                        cache.unavailableProxyAmount(), cache.size(Proxy.class))
        ).persist();
    }

    private void check() {
        cache.firstNotCheckedProxy().ifPresentOrElse(this::check, () -> runLater(this::check));
    }

    private void check(Proxy proxy) {
        final long startTime = nowMillis();
        request(proxy)
                .exceptionally(Utils::throwableHandler)
                .thenAccept(response -> {
                    proxy.latency = response != null && response.statusCode() == 200 ? nowMillis() - startTime : Long.MAX_VALUE;
                    cache.upsert(proxy);
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
