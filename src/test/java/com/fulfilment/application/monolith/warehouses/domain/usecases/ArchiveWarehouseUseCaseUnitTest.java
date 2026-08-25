package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link ArchiveWarehouseUseCase}, with {@link WarehouseStore} mocked.
 * See {@link ArchiveWarehouseUseCaseTest} for the corresponding @QuarkusTest-based
 * database/concurrency-integration coverage.
 */
public class ArchiveWarehouseUseCaseUnitTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  private Warehouse existingWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "WH-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;
    warehouse.createdAt = LocalDateTime.now();
    return warehouse;
  }

  @Test
  public void archivesExistingActiveWarehouse() {
    Warehouse existing = existingWarehouse();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "WH-001";
    useCase.archive(request);

    assertNotNull(existing.archivedAt, "archivedAt should be stamped");
    verify(warehouseStore, times(1)).update(existing);
  }

  @Test
  public void rejectsArchivingNonExistentWarehouse() {
    when(warehouseStore.findByBusinessUnitCode("MISSING")).thenReturn(null);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "MISSING";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.archive(request));

    assertTrue(ex.getMessage().contains("does not exist"));
    verify(warehouseStore, never()).update(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void rejectsArchivingAlreadyArchivedWarehouse() {
    Warehouse existing = existingWarehouse();
    existing.archivedAt = LocalDateTime.now().minusDays(1);
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "WH-001";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.archive(request));

    assertTrue(ex.getMessage().contains("already archived"));
    verify(warehouseStore, never()).update(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void rejectsMissingBusinessUnitCode() {
    Warehouse request = new Warehouse();
    request.businessUnitCode = null;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.archive(request));

    assertEquals("businessUnitCode is required", ex.getMessage());
  }
}
