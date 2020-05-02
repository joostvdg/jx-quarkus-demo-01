package com.github.joostvdg.jx.quarkus.fruits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/fruits")
public class FruitResource {

    private final Logger log = LoggerFactory.getLogger(FruitResource.class);


    private final FruitRepository fruitRepository;

    public FruitResource(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

//    @GetMapping("/")
    @RequestMapping(path = "/", method = RequestMethod.GET, produces = "application/json")
    public List<Fruit> findAll() {
        var it = fruitRepository.findAll();
        var fruits = new ArrayList<Fruit>();
        it.forEach(fruits::add);
        return fruits;
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Long> delete(@PathVariable(value = "id") long id) {
        try {
            fruitRepository.deleteById(id);
        } catch (IllegalArgumentException iae) {
            log.warn("Could not delete fruit with id: " + id, iae);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(id, HttpStatus.NO_CONTENT);
    }

    @PostMapping("/name/{name}/color/{color}")
    public Fruit create(@PathVariable(value = "name") String name, @PathVariable(value = "color") String color) {
        return fruitRepository.save(new Fruit(name, color));
    }

    @PutMapping("/id/{id}/color/{color}")
    public Fruit changeColor(@PathVariable(value = "id") Long id, @PathVariable(value = "color") String color) {
        Optional<Fruit> optional = fruitRepository.findById(id);
        if (optional.isPresent()) {
            Fruit fruit = optional.get();
            fruit.setColor(color);
            return fruitRepository.save(fruit);
        }

        throw new IllegalArgumentException("No Fruit with id " + id + " exists");
    }

    @GetMapping("/color/{color}")
    public List<Fruit> findByColor(@PathVariable(value = "color") String color) {
        return fruitRepository.findByColor(color);
    }
}