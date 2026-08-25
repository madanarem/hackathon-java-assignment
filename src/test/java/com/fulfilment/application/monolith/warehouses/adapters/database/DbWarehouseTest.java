package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test for {@link DbWarehouse#toWarehouse()}, the JPA entity &lt;-&gt; domain
 * model mapping used throughout {@link WarehouseRepository}.
 */
public class DbWarehouseTest {

  @Test
  public void mapsAllFieldsToDomainModel() {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = "WH-001";
    entity.location = "AMSTERDAM-001";
    entity.capacity = 50;
    entity.stock = 10;
    entity.createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
    entity.archivedAt = LocalDateTime.of(2024, 6, 1, 0, 0);

    var domain = entity.toWarehouse();

    assertEquals("WH-001", domain.businessUnitCode);
    assertEquals("AMSTERDAM-001", domain.location);
    assertEquals(50, domain.capacity);
    assertEquals(10, domain.stock);
    assertEquals(entity.createdAt, domain.createdAt);
    assertEquals(entity.archivedAt, domain.archivedAt);
  }

  @Test
  public void mapsNullArchivedAtForActiveWarehouse() {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = "WH-002";
    entity.archivedAt = null;

    var domain = entity.toWarehouse();

    assertNull(domain.archivedAt);
  }
}
