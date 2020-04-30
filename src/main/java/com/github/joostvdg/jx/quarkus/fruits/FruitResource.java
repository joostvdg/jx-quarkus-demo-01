package com.github.joostvdg.jx.quarkus.fruits;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fruits")
public class FruitResource {

    private final FruitRepository fruitRepository;

    public FruitResource(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

    @GetMapping
    public Iterable<Fruit> findAll() {
        return fruitRepository.findAll();
    }

    @DeleteMapping("{id}")
    public void delete(@PathParam long id) {
        fruitRepository.deleteById(id);
    }

    @PostMapping("/name/{name}/color/{color}")
    public Fruit create(@PathParam String name, @PathParam String color) {
        return fruitRepository.save(new Fruit(name, color));
    }

    @PutMapping("/id/{id}/color/{color}")
    public Fruit changeColor(@PathParam Long id, @PathParam String color) {
        Optional<Fruit> optional = fruitRepository.findById(id);
        if (optional.isPresent()) {
            Fruit fruit = optional.get();
            fruit.setColor(color);
            return fruitRepository.save(fruit);
        }

        throw new IllegalArgumentException("No Fruit with id " + id + " exists");
    }

    @GetMapping
    @Path("/color/{color}")
    @Produces("application/json")
    public List<Fruit> findByColor(@PathParam String color) {
        return fruitRepository.findByColor(color);
    }
}