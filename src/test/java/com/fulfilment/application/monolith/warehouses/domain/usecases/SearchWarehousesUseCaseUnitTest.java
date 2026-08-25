package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseSearchCriteria;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Pure unit tests for {@link SearchWarehousesUseCase}: verifies defaulting/normalization
 * of sort/paging parameters before delegating to {@link WarehouseStore}.
 */
public class SearchWarehousesUseCaseUnitTest {

  private WarehouseStore warehouseStore;
  private SearchWarehousesUseCase useCase;

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    useCase = new SearchWarehousesUseCase(warehouseStore);
    when(warehouseStore.search(any())).thenReturn(List.of(new Warehouse()));
  }

  @Test
  public void defaultsInvalidSortByToCreatedAt() {
    useCase.search(new WarehouseSearchCriteria(null, null, null, "unknownField", "asc", 0, 10));

    WarehouseSearchCriteria used = capturedCriteria();
    assertEquals("createdAt", used.sortBy());
  }

  @Test
  public void acceptsCapacityAsSortableField() {
    useCase.search(new WarehouseSearchCriteria(null, null, null, "capacity", "desc", 0, 10));

    WarehouseSearchCriteria used = capturedCriteria();
    assertEquals("capacity", used.sortBy());
    assertEquals("desc", used.sortOrder());
  }

  @Test
  public void defaultsInvalidSortOrderToAsc() {
    useCase.search(new WarehouseSearchCriteria(null, null, null, "capacity", "sideways", 0, 10));

    assertEquals("asc", capturedCriteria().sortOrder());
  }

  @Test
  public void clampsNegativePageToZero() {
    useCase.search(new WarehouseSearchCriteria(null, null, null, "createdAt", "asc", -5, 10));

    assertEquals(0, capturedCriteria().page());
  }

  @Test
  public void defaultsNonPositivePageSizeToTen() {
    useCase.search(new WarehouseSearchCriteria(null, null, null, "createdAt", "asc", 0, 0));

    assertEquals(10, capturedCriteria().pageSize());
  }

  @Test
  public void clampsPageSizeAboveMaxToOneHundred() {
    useCase.search(new WarehouseSearchCriteria(null, null, null, "createdAt", "asc", 0, 500));

    assertEquals(100, capturedCriteria().pageSize());
  }

  @Test
  public void rejectsMinCapacityGreaterThanMaxCapacity() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.search(new WarehouseSearchCriteria(null, 80, 20, "createdAt", "asc", 0, 10)));

    assertEquals("minCapacity cannot be greater than maxCapacity", ex.getMessage());
  }

  @Test
  public void passesThroughLocationAndCapacityFilters() {
    useCase.search(new WarehouseSearchCriteria("AMSTERDAM-001", 10, 90, "createdAt", "asc", 0, 10));

    WarehouseSearchCriteria used = capturedCriteria();
    assertEquals("AMSTERDAM-001", used.location());
    assertEquals(10, used.minCapacity());
    assertEquals(90, used.maxCapacity());
  }

  private WarehouseSearchCriteria capturedCriteria() {
    ArgumentCaptor<WarehouseSearchCriteria> captor = ArgumentCaptor.forClass(WarehouseSearchCriteria.class);
    verify(warehouseStore).search(captor.capture());
    return captor.getValue();
  }
}
