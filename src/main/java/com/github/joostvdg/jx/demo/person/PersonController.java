package com.github.joostvdg.jx.demo.person;

/*
 * Copyright 2019 Google LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
import io.quarkus.panache.common.Sort;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

/**
 * An example JAX-RS controller that uses Panache repository for CRUD operations with a JPA entity.
 *
 * <p>Optionally, you can also inject an {@link EntityManager} for direct JPA access.
 */
@Path("/person")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonController {

    /**
     * Create a new {@link Person} entity with auto-generated ID.
     * @param person JSON payload for Person
     * @return created {@link Person} entity
     */
    @POST
    @Path("/")
    @Transactional
    public Response create(Person person) {
        if (person.id != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        person.persist();
        return Response.ok(person).status(201).build();
    }

    /**
     * Retrieve all {@link Person} entities.
     * @return a {@link Person} entity
     */
    @GET
    public List<Person> get() {
        return Person.listAll(Sort.by("name"));
    }

    /**
     * Retrieve a {@link Person} entity by ID.
     * @param id UUID String to lookup
     * @return a {@link Person} entity
     */
    @GET
    @Path("{id}")
    public Person getSingle(@PathParam Long id) {
        Person entity = Person.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Fruit with id of " + id + " does not exist.", 404);
        }
        return entity;
    }
}