package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * REST-level tests for {@link StoreResource}, covering positive and negative/error
 * conditions (validation failures, not-found). The legacy gateway is mocked so these
 * tests don't depend on transaction-commit timing (see {@link StoreTransactionIntegrationTest}
 * for that concern specifically).
 */
@QuarkusTest
public class StoreResourceTest {

  @InjectMock LegacyStoreManagerGateway legacyGateway;

  @Test
  public void testGetNonExistentStoreReturns404() {
    given().when().get("/store/999999").then().statusCode(404);
  }

  @Test
  public void testCreateStoreWithIdSetReturns422() {
    given()
        .contentType("application/json")
        .body("{\"id\": 1, \"name\": \"INVALID\", \"quantityProductsInStock\": 1}")
        .when()
        .post("/store")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateStoreWithMissingNameReturns422() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 5}")
        .when()
        .put("/store/1")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateNonExistentStoreReturns404() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"DOES-NOT-EXIST\", \"quantityProductsInStock\": 5}")
        .when()
        .put("/store/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteNonExistentStoreReturns404() {
    given().when().delete("/store/999999").then().statusCode(404);
  }

  @Test
  public void testCreateStoreSucceedsAndIsRetrievable() {
    String uniqueName = "STORE-" + System.nanoTime();

    int newId =
        given()
            .contentType("application/json")
            .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 3}")
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .body("name", equalTo(uniqueName))
            .extract()
            .path("id");

    given().when().get("/store/" + newId).then().statusCode(200).body("name", equalTo(uniqueName));
  }

  @Test
  public void testUpdateStoreSucceeds() {
    String uniqueName = "STORE-UPD-" + System.nanoTime();

    int newId =
        given()
            .contentType("application/json")
            .body("{\"name\": \"" + uniqueName + "\", \"quantityProductsInStock\": 3}")
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String updatedName = uniqueName + "-UPDATED";
    given()
        .contentType("application/json")
        .body("{\"name\": \"" + updatedName + "\", \"quantityProductsInStock\": 9}")
        .when()
        .put("/store/" + newId)
        .then()
        .statusCode(200)
        .body("name", equalTo(updatedName))
        .body("quantityProductsInStock", equalTo(9));
  }
}
