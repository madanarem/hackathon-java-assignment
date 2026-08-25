package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // Validation 0: Required fields must be present
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("businessUnitCode is required");
    }
    if (warehouse.capacity == null) {
      throw new IllegalArgumentException("capacity is required");
    }
    if (warehouse.stock == null) {
      throw new IllegalArgumentException("stock is required");
    }

    // Validation 1: Business unit code must be unique
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      LOGGER.warnf("Rejected create: business unit code '%s' already exists", warehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists");
    }

    // Validation 2: Location must be valid (must exist)
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      LOGGER.warnf("Rejected create: location '%s' is not valid", warehouse.location);
      throw new IllegalArgumentException(
          "Location '" + warehouse.location + "' is not valid");
    }

    // Validation 3: Capacity validation
    // - Capacity cannot exceed location's max capacity
    if (warehouse.capacity > location.maxCapacity()) {
      LOGGER.warnf(
          "Rejected create for '%s': capacity %d exceeds location max capacity %d",
          warehouse.businessUnitCode, warehouse.capacity, location.maxCapacity());
      throw new IllegalArgumentException(
          "Warehouse capacity (" + warehouse.capacity +
          ") exceeds location max capacity (" + location.maxCapacity() + ")");
    }

    // - Stock cannot exceed capacity
    if (warehouse.stock > warehouse.capacity) {
      LOGGER.warnf(
          "Rejected create for '%s': stock %d exceeds capacity %d",
          warehouse.businessUnitCode, warehouse.stock, warehouse.capacity);
      throw new IllegalArgumentException(
          "Warehouse stock (" + warehouse.stock +
          ") exceeds warehouse capacity (" + warehouse.capacity + ")");
    }

    // Set creation timestamp
    warehouse.createdAt = java.time.LocalDateTime.now();

    // All validations passed, create the warehouse
    warehouseStore.create(warehouse);
    LOGGER.infof("Created warehouse '%s' at location '%s'", warehouse.businessUnitCode, warehouse.location);
  }
}
