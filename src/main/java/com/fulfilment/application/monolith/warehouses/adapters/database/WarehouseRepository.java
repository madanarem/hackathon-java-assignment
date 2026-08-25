package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseSearchCriteria;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  @Transactional
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }

    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;

    // Flush now so a concurrent modification (@Version mismatch) surfaces as an
    // OptimisticLockException here rather than silently overwriting the other change,
    // then clear the persistence context so subsequent reads in this transaction see fresh data.
    getEntityManager().flush();
    getEntityManager().clear();
  }

  @Override
  public void remove(Warehouse warehouse) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'remove'");
  }

  @Override
  @Transactional
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }

  @Override
  @Transactional
  public List<Warehouse> search(WarehouseSearchCriteria criteria) {
    StringBuilder query = new StringBuilder("archivedAt is null");
    Parameters params = new Parameters();

    if (criteria.location() != null && !criteria.location().isBlank()) {
      query.append(" and location = :location");
      params.and("location", criteria.location());
    }
    if (criteria.minCapacity() != null) {
      query.append(" and capacity >= :minCapacity");
      params.and("minCapacity", criteria.minCapacity());
    }
    if (criteria.maxCapacity() != null) {
      query.append(" and capacity <= :maxCapacity");
      params.and("maxCapacity", criteria.maxCapacity());
    }

    Sort sort =
        Sort.by(criteria.sortBy(), "desc".equals(criteria.sortOrder()) ? Sort.Direction.Descending : Sort.Direction.Ascending);

    return find(query.toString(), sort, params.map())
        .page(Page.of(criteria.page(), criteria.pageSize()))
        .stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
}
