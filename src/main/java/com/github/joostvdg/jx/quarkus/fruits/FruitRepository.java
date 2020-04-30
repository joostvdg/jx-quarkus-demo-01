package com.github.joostvdg.jx.quarkus.fruits;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface FruitRepository extends CrudRepository<Fruit, Long> {

    List<Fruit> findByColor(String color);
}