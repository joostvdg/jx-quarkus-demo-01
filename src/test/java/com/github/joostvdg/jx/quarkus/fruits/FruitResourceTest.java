package com.github.joostvdg.jx.quarkus.fruits;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTest
class FruitResourceTest {
//.header("Content-Type", "application/json")
//                .header("Accept", "application/json")

    @Test
    void testListAllFruits() {
        //List all, should have all 3 fruits the database has initially:
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        containsString("Cherry"),
                        containsString("Apple"),
                        containsString("Banana"));
        //Delete the Cherry:
        given()
                .when().delete("/fruits/1")
                .then()
                .statusCode(204);

        //List all, cherry should be missing now:
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        not(containsString("Cherry")),
                        containsString("Apple"),
                        containsString("Banana"));

        //Create a new Fruit
        given()
                .when().post("/fruits/name/Orange/color/Orange")
                .then()
                .statusCode(200)
                .body(containsString("Orange"))
                .body("id", notNullValue())
                .extract().body().jsonPath().getString("id");

        //List all, Orange should be present now:
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        not(containsString("Cherry")),
                        containsString("Apple"),
                        containsString("Orange"));
    }

    @Test
    void testFindByColor() {
        //Find by color that no fruit has
        given()
                .when().get("/fruits/color/Black")
                .then()
                .statusCode(200)
                .body("size()", is(0));

        //Find by color that multiple fruits have
        given()
                .when().get("/fruits/color/Red")
                .then()
                .statusCode(200)
                .body(
                        containsString("Apple"),
                        containsString("Strawberry"));

        //Find by color that matches
        given()
                .when().get("/fruits/color/Green")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Avocado"));

        //Update color of Avocado
        given()
                .when().put("/fruits/id/4/color/Black")
                .then()
                .statusCode(200)
                .body(containsString("Black"));

        //Find by color that Avocado now has
        given()
                .when().get("/fruits/color/Black")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body(
                        containsString("Black"),
                        containsString("Avocado"));
    }

}