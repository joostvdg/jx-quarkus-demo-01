package com.github.joostvdg.jx.quarkus.fruits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.jboss.logging.Logger;

@RestController
@RequestMapping(value = "/fruits")
public class FruitResource {

    private static final Logger LOG = Logger.getLogger(FruitResource.class);

    private final FruitRepository fruitRepository;

    public FruitResource(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

    @GetMapping("/")
    @Counted(name = "fruit_get_all_count", description = "How many times all Fruits have been retrieved.")
    @Timed(name = "fruit_get_all_timer", description = "A measure of how long it takes to retrieve all Fruits.", unit = MetricUnits.MILLISECONDS)
    public List<Fruit> findAll() {
        var it = fruitRepository.findAll();
        var fruits = new ArrayList<Fruit>();
        it.forEach(fruits::add);
        fruits.sort(Comparator.comparing(Fruit::getId));
        LOG.infof("Found %s fruits", fruits.size());
        return fruits;
    }

    @DeleteMapping("{id}")
    @Counted(name = "fruit_delete_count", description = "How many times a Fruit has been deleted.")
    @Timed(name = "fruit_delete_timer", description = "A measure of how long it takes to delete a Fruit.", unit = MetricUnits.MILLISECONDS)
    public ResponseEntity<Long> delete(@PathVariable(value = "id") long id) {
        try {
            fruitRepository.deleteById(id);
            LOG.infof("Deleted fruit by id: %d", id);
        } catch (IllegalArgumentException iae) {
            LOG.warn("Could not delete fruit with id: " + id, iae);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(id, HttpStatus.NO_CONTENT);
    }

    @Counted(name = "fruit_new_count", description = "How many times a Fruit has been saved.")
    @Timed(name = "fruit_new_timer", description = "A measure of how long it takes to save a Fruit.", unit = MetricUnits.MILLISECONDS)
    @PostMapping("/name/{name}/color/{color}")
    public Fruit create(@PathVariable(value = "name") String name, @PathVariable(value = "color") String color) {
        Fruit newFruit = fruitRepository.save(new Fruit(name, color));
        LOG.infof("Created new Fruit: name=%s, color=%s", name, color);
        return newFruit;
    }

    @Counted(name = "fruit_update_count", description = "How many times a Fruit has been updated.")
    @Timed(name = "fruit_dupdate_timer", description = "A measure of how long it takes to update a Fruit.", unit = MetricUnits.MILLISECONDS)
    @PutMapping("/id/{id}/color/{color}")
    public Fruit changeColor(@PathVariable(value = "id") Long id, @PathVariable(value = "color") String color) {
        Optional<Fruit> optional = fruitRepository.findById(id);
        if (optional.isPresent()) {
            Fruit fruit = optional.get();
            fruit.setColor(color);
            LOG.infof("Updated Fruit with id %d, to color %s", id, color);
            return fruitRepository.save(fruit);
        }

        throw new IllegalArgumentException("No Fruit with id " + id + " exists");
    }

    @Counted(name = "fruit_getbycolor_count", description = "How many times Fruits have been retrieved by color.")
    @Timed(name = "fruit_getbycolor_timer", description = "A measure of how long it takes to retrieve Fruits by color.", unit = MetricUnits.MILLISECONDS)
    @GetMapping("/color/{color}")
    public List<Fruit> findByColor(@PathVariable(value = "color") String color) {
        List<Fruit> foundFruit = fruitRepository.findByColor(color);
        LOG.infof("Found %d fruits with the color %s", foundFruit.size(), color);
        return foundFruit;
    }
}