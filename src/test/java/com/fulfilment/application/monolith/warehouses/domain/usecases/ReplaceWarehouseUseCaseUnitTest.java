package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link ReplaceWarehouseUseCase}, with its ports mocked. See
 * {@link ReplaceWarehouseUseCaseTest} for the corresponding @QuarkusTest-based
 * database/concurrency-integration coverage.
 */
public class ReplaceWarehouseUseCaseUnitTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse existingWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "WH-001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;
    warehouse.stock = 5;
    warehouse.createdAt = LocalDateTime.now();
    return warehouse;
  }

  private Warehouse replacement() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "WH-001";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 60;
    replacement.stock = 20;
    return replacement;
  }

  @Test
  public void replacesExistingActiveWarehouse() {
    Warehouse existing = existingWarehouse();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse replacement = replacement();
    useCase.replace(replacement);

    assertEquals("AMSTERDAM-001", existing.location);
    assertEquals(60, existing.capacity);
    assertEquals(20, existing.stock);
    verify(warehouseStore, times(1)).update(existing);
  }

  @Test
  public void rejectsReplacingNonExistentWarehouse() {
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement()));

    assertTrue(ex.getMessage().contains("does not exist"));
    verify(warehouseStore, never()).update(any());
  }

  @Test
  public void rejectsReplacingArchivedWarehouse() {
    Warehouse existing = existingWarehouse();
    existing.archivedAt = LocalDateTime.now();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement()));

    assertTrue(ex.getMessage().contains("archived"));
    verify(warehouseStore, never()).update(any());
  }

  @Test
  public void rejectsUnknownLocation() {
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existingWarehouse());
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(null);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement()));

    assertTrue(ex.getMessage().contains("not valid"));
  }

  @Test
  public void rejectsCapacityExceedingLocationMax() {
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existingWarehouse());
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse replacement = replacement();
    replacement.capacity = 500;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));

    assertTrue(ex.getMessage().contains("exceeds location max capacity"));
  }

  @Test
  public void rejectsStockExceedingCapacity() {
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existingWarehouse());
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse replacement = replacement();
    replacement.stock = 999;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));

    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  public void rejectsMissingBusinessUnitCode() {
    Warehouse replacement = replacement();
    replacement.businessUnitCode = null;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));

    assertEquals("businessUnitCode is required", ex.getMessage());
  }

  @Test
  public void rejectsMissingCapacity() {
    Warehouse replacement = replacement();
    replacement.capacity = null;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));

    assertEquals("capacity is required", ex.getMessage());
  }

  @Test
  public void rejectsMissingStock() {
    Warehouse replacement = replacement();
    replacement.stock = null;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));

    assertEquals("stock is required", ex.getMessage());
  }
}
