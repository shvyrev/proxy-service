package org.acme;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;
import org.acme.model.Proxy;
import org.acme.utils.Utils;
import org.acme.workers.HTTPWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/")
public class ExampleResource {

    @Inject
    Cache cache;

    private static final Logger log = LoggerFactory.getLogger( ExampleResource.class );

    @Inject
    HTTPWorker http;

    @Scheduled(every = "20m")
    void wakeMeUp(){
        http.getHtml("https://appmobiles-proxy-service.herokuapp.com/ping")
                .exceptionally(Utils::throwableHandler)
                .thenAccept(s -> log.info(" $ wakeMeUp : " + s));
    }

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping(){
        return ok("pong").build();
    }

    @GET
    @Path("proxy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxy(){
        return ok(cache.randomMinLatencyProxy()).build();
    }

    @GET
    @Path("stat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stat(){
        return ok(
                new JsonObject()
                        .put("proxies", cache.size(Proxy.class))
                        .put("cpu", Runtime.getRuntime().availableProcessors())
                        .put("proxiesChecked", cache.checkedProxyAmount())
        ).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}