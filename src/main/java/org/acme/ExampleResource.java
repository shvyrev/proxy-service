package org.acme;

import org.acme.model.Proxy;
import org.acme.model.Report;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

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
    @Path("report")
    @Produces(MediaType.APPLICATION_JSON)
    public Optional<Report> report(){
        return cache.lastReport().map(report -> report.finish(cache.checkedProxyAmount(), cache.availableProxyAmount(),
                cache.unavailableProxyAmount(), cache.size(Proxy.class)));
    }

    @GET
    @Path("proxy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxy(){
        return ok(cache.randomMinLatencyProxy()).build();
    }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}