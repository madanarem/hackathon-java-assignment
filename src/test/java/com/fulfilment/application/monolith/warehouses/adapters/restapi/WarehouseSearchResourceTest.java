package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the bonus GET /warehouse/search endpoint.
 */
@QuarkusTest
public class WarehouseSearchResourceTest {

  @Inject WarehouseRepository warehouseRepository;
  @Inject CreateWarehouseUseCase createWarehouseUseCase;
  @Inject ArchiveWarehouseUseCase archiveWarehouseUseCase;
  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();

    create("SEARCH-AMS-1", "AMSTERDAM-001", 30);
    create("SEARCH-AMS-2", "AMSTERDAM-001", 60);
    create("SEARCH-AMS-3", "AMSTERDAM-001", 90);
    create("SEARCH-ZWO-1", "ZWOLLE-001", 40);

    // Archived warehouses must never show up in search results
    Warehouse archived = create("SEARCH-ARCHIVED", "ZWOLLE-002", 20);
    archiveWarehouseUseCase.archive(archived);
  }

  private Warehouse create(String code, String location, int capacity) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = 1;
    warehouse.createdAt = LocalDateTime.now();
    createWarehouseUseCase.create(warehouse);
    return warehouse;
  }

  @Test
  public void testFilterByLocation() {
    given()
        .when()
        .queryParam("location", "AMSTERDAM-001")
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", equalTo(3))
        .body("location", everyItem(equalTo("AMSTERDAM-001")));
  }

  @Test
  public void testExcludesArchivedWarehouses() {
    given()
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("businessUnitCode", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("SEARCH-ARCHIVED")));
  }

  @Test
  public void testFilterByCapacityRangeIsAndCombined() {
    given()
        .when()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("minCapacity", 40)
        .queryParam("maxCapacity", 80)
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].businessUnitCode", equalTo("SEARCH-AMS-2"));
  }

  @Test
  public void testSortByCapacityDescending() {
    given()
        .when()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("[0].capacity", equalTo(90))
        .body("[1].capacity", equalTo(60))
        .body("[2].capacity", equalTo(30));
  }

  @Test
  public void testPagination() {
    given()
        .when()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .queryParam("page", 1)
        .queryParam("pageSize", 2)
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].capacity", equalTo(90));
  }

  @Test
  public void testPageSizeIsRespected() {
    given()
        .when()
        .queryParam("pageSize", 2)
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("$", hasSize(lessThanOrEqualTo(2)));
  }
}
