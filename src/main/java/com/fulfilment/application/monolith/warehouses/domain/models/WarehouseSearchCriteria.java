package com.fulfilment.application.monolith.warehouses.domain.models;

public record WarehouseSearchCriteria(
    String location,
    Integer minCapacity,
    Integer maxCapacity,
    String sortBy,
    String sortOrder,
    int page,
    int pageSize) {}
