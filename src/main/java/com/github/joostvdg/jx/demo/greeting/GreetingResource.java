package com.github.joostvdg.jx.demo.greeting;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.inject.Inject;
import org.jboss.resteasy.annotations.jaxrs.PathParam;


@Path("/greeting")
public class GreetingResource {

  @Inject GreetingService service;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{name}")
  public String greeting(@PathParam String name) {
    return service.greeting(name);
  }
}