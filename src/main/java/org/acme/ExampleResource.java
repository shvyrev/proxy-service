package org.acme;

import io.vertx.core.json.JsonObject;
import org.acme.model.Proxy;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/")
public class ExampleResource {

    @Inject
    Cache cache;

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