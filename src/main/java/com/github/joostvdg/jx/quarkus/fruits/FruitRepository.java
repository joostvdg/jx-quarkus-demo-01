package com.github.joostvdg.jx.quarkus.fruits;

import java.util.List;

import org.eclipse.microprofile.opentracing.Traced;
import org.springframework.data.repository.CrudRepository;

@Traced
public interface FruitRepository extends CrudRepository<Fruit, Long> {

    List<Fruit> findByColor(String color);
}