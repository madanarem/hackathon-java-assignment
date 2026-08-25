package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link CreateWarehouseUseCase}, with its ports (WarehouseStore,
 * LocationResolver) mocked via Mockito. No Quarkus context or database involved -
 * covers the use case's business rules in isolation, for positive, negative and
 * error/edge-case scenarios.
 */
public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse validWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "WH-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;
    return warehouse;
  }

  // --- positive case ---

  @Test
  public void createsWarehouseWhenAllValidationsPass() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    useCase.create(warehouse);

    assertNotNull(warehouse.createdAt, "createdAt should be stamped on successful creation");
    verify(warehouseStore, times(1)).create(warehouse);
  }

  // --- negative cases: business rule violations ---

  @Test
  public void rejectsDuplicateBusinessUnitCode() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(validWarehouse());

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertTrue(ex.getMessage().contains("already exists"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void rejectsUnknownLocation() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertTrue(ex.getMessage().contains("not valid"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void rejectsCapacityExceedingLocationMax() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = 150;
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void rejectsStockExceedingCapacity() {
    Warehouse warehouse = validWarehouse();
    warehouse.stock = 999;
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
    verify(warehouseStore, never()).create(any());
  }

  // --- error/edge cases: missing required fields ---

  @Test
  public void rejectsMissingBusinessUnitCode() {
    Warehouse warehouse = validWarehouse();
    warehouse.businessUnitCode = null;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertEquals("businessUnitCode is required", ex.getMessage());
  }

  @Test
  public void rejectsBlankBusinessUnitCode() {
    Warehouse warehouse = validWarehouse();
    warehouse.businessUnitCode = "   ";

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void rejectsMissingCapacity() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = null;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertEquals("capacity is required", ex.getMessage());
  }

  @Test
  public void rejectsMissingStock() {
    Warehouse warehouse = validWarehouse();
    warehouse.stock = null;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));

    assertEquals("stock is required", ex.getMessage());
  }

  // --- boundary cases ---

  @Test
  public void acceptsCapacityExactlyAtLocationMax() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = 100;
    warehouse.stock = 100;
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    useCase.create(warehouse);

    verify(warehouseStore, times(1)).create(warehouse);
  }
}
