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
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;

/**
 * A Panache CRUD repository for the {@link Person} entity.
 */
@ApplicationScoped
public class PersonRepository implements PanacheRepositoryBase<Person, UUID> {}