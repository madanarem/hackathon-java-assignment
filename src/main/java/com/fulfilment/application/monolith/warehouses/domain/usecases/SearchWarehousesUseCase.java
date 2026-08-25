package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseSearchCriteria;
import com.fulfilment.application.monolith.warehouses.domain.ports.SearchWarehousesOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SearchWarehousesUseCase implements SearchWarehousesOperation {

  private static final Logger LOGGER = Logger.getLogger(SearchWarehousesUseCase.class);
  private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "capacity");
  private static final int MAX_PAGE_SIZE = 100;
  private static final int DEFAULT_PAGE_SIZE = 10;

  private final WarehouseStore warehouseStore;

  public SearchWarehousesUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public List<Warehouse> search(WarehouseSearchCriteria criteria) {
    if (criteria.minCapacity() != null
        && criteria.maxCapacity() != null
        && criteria.minCapacity() > criteria.maxCapacity()) {
      throw new IllegalArgumentException("minCapacity cannot be greater than maxCapacity");
    }

    String sortBy = SORTABLE_FIELDS.contains(criteria.sortBy()) ? criteria.sortBy() : "createdAt";
    String sortOrder = "desc".equalsIgnoreCase(criteria.sortOrder()) ? "desc" : "asc";
    int page = Math.max(criteria.page(), 0);
    int pageSize = criteria.pageSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(criteria.pageSize(), MAX_PAGE_SIZE);

    WarehouseSearchCriteria normalized =
        new WarehouseSearchCriteria(
            criteria.location(), criteria.minCapacity(), criteria.maxCapacity(), sortBy, sortOrder, page, pageSize);

    List<Warehouse> results = warehouseStore.search(normalized);
    LOGGER.debugf(
        "Search returned %d warehouse(s) for location=%s, minCapacity=%s, maxCapacity=%s, page=%d, pageSize=%d",
        results.size(), criteria.location(), criteria.minCapacity(), criteria.maxCapacity(), page, pageSize);
    return results;
  }
}
