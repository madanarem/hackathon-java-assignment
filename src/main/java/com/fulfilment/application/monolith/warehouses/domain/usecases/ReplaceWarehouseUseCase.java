package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse.businessUnitCode == null || newWarehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("businessUnitCode is required");
    }
    if (newWarehouse.capacity == null) {
      throw new IllegalArgumentException("capacity is required");
    }
    if (newWarehouse.stock == null) {
      throw new IllegalArgumentException("stock is required");
    }

    // Validation 1: Warehouse must exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      LOGGER.warnf("Rejected replace: business unit code '%s' does not exist", newWarehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' does not exist");
    }

    // Validation 2: Warehouse must not be archived
    if (existing.archivedAt != null) {
      LOGGER.warnf("Rejected replace: '%s' is archived", newWarehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + newWarehouse.businessUnitCode + "' is archived and cannot be replaced");
    }

    // Validation 3: Location must be valid
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      LOGGER.warnf("Rejected replace: location '%s' is not valid", newWarehouse.location);
      throw new IllegalArgumentException(
          "Location '" + newWarehouse.location + "' is not valid");
    }

    // Validation 4: Capacity validation
    // - Capacity cannot exceed location's max capacity
    if (newWarehouse.capacity > location.maxCapacity()) {
      LOGGER.warnf(
          "Rejected replace for '%s': capacity %d exceeds location max capacity %d",
          newWarehouse.businessUnitCode, newWarehouse.capacity, location.maxCapacity());
      throw new IllegalArgumentException(
          "Warehouse capacity (" + newWarehouse.capacity +
          ") exceeds location max capacity (" + location.maxCapacity() + ")");
    }

    // - Stock cannot exceed capacity
    if (newWarehouse.stock > newWarehouse.capacity) {
      LOGGER.warnf(
          "Rejected replace for '%s': stock %d exceeds capacity %d",
          newWarehouse.businessUnitCode, newWarehouse.stock, newWarehouse.capacity);
      throw new IllegalArgumentException(
          "Warehouse stock (" + newWarehouse.stock +
          ") exceeds warehouse capacity (" + newWarehouse.capacity + ")");
    }

    // Update warehouse fields (preserve createdAt, businessUnitCode, archivedAt)
    existing.location = newWarehouse.location;
    existing.capacity = newWarehouse.capacity;
    existing.stock = newWarehouse.stock;

    // Update the warehouse
    warehouseStore.update(existing);
    LOGGER.infof("Replaced warehouse '%s'", newWarehouse.businessUnitCode);
  }
}
