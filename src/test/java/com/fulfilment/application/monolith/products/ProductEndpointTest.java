package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  // --- negative / error condition tests ---

  @Test
  public void testGetNonExistentProductReturns404() {
    given().when().get("/product/99999").then().statusCode(404);
  }

  @Test
  public void testCreateProductWithIdSetReturns422() {
    given()
        .contentType("application/json")
        .body("{\"id\": 1, \"name\": \"INVALID\", \"stock\": 1}")
        .when()
        .post("/product")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateProductWithMissingNameReturns422() {
    given()
        .contentType("application/json")
        .body("{\"stock\": 5}")
        .when()
        .put("/product/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateNonExistentProductReturns404() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"DOES-NOT-EXIST\", \"stock\": 5}")
        .when()
        .put("/product/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteNonExistentProductReturns404() {
    given().when().delete("/product/99999").then().statusCode(404);
  }

  @Test
  public void testCreateProductSucceedsAndIsRetrievable() {
    int newId =
        given()
            .contentType("application/json")
            .body("{\"name\": \"NEW-PRODUCT\", \"stock\": 7}")
            .when()
            .post("/product")
            .then()
            .statusCode(201)
            .body("name", org.hamcrest.Matchers.equalTo("NEW-PRODUCT"))
            .extract()
            .path("id");

    given().when().get("/product/" + newId).then().statusCode(200).body("name", org.hamcrest.Matchers.equalTo("NEW-PRODUCT"));
  }
}
