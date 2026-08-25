package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseSearchCriteria;
import java.util.List;

public interface SearchWarehousesOperation {

  List<Warehouse> search(WarehouseSearchCriteria criteria);
}
