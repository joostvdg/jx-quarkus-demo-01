package com.github.joostvdg.jx.demo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.inject.Inject;

import com.github.joostvdg.jx.demo.greeting.GreetingService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;


@Path("/hello")
public class ExampleResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}