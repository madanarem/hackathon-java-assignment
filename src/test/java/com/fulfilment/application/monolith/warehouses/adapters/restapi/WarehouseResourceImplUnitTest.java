package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.SearchWarehousesOperation;
import com.warehouse.api.beans.Warehouse;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for {@link WarehouseResourceImpl}: verifies REST-layer wiring
 * (API bean &lt;-&gt; domain model mapping, HTTP status translation for not-found and
 * validation failures) in isolation from the database and Quarkus context.
 */
@ExtendWith(MockitoExtension.class)
public class WarehouseResourceImplUnitTest {

  @Mock private WarehouseRepository warehouseRepository;
  @Mock private CreateWarehouseOperation createWarehouseOperation;
  @Mock private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Mock private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Mock private SearchWarehousesOperation searchWarehousesOperation;

  private WarehouseResourceImpl resource;

  @BeforeEach
  public void setup() throws Exception {
    resource = new WarehouseResourceImpl();
    setField("warehouseRepository", warehouseRepository);
    setField("createWarehouseOperation", createWarehouseOperation);
    setField("archiveWarehouseOperation", archiveWarehouseOperation);
    setField("replaceWarehouseOperation", replaceWarehouseOperation);
    setField("searchWarehousesOperation", searchWarehousesOperation);
  }

  private void setField(String name, Object value) throws Exception {
    var field = WarehouseResourceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(resource, value);
  }

  // --- listAllWarehousesUnits ---

  @Test
  public void listReturnsAllWarehousesMappedToApiBeans() {
    var warehouse1 = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse1.businessUnitCode = "WH-001";
    var warehouse2 = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse2.businessUnitCode = "WH-002";
    when(warehouseRepository.getAll()).thenReturn(List.of(warehouse1, warehouse2));

    List<Warehouse> response = resource.listAllWarehousesUnits();

    assertEquals(2, response.size());
    assertEquals("WH-001", response.get(0).getBusinessUnitCode());
    assertEquals("WH-002", response.get(1).getBusinessUnitCode());
  }

  // --- createANewWarehouseUnit ---

  @Test
  public void createReturnsMappedWarehouseOnSuccess() {
    Warehouse request = new Warehouse();
    request.setBusinessUnitCode("WH-001");
    request.setLocation("AMSTERDAM-001");
    request.setCapacity(50);
    request.setStock(10);

    Warehouse response = resource.createANewWarehouseUnit(request);

    assertEquals("WH-001", response.getBusinessUnitCode());
    assertEquals("AMSTERDAM-001", response.getLocation());
    assertEquals(50, response.getCapacity());
    assertEquals(10, response.getStock());
  }

  @Test
  public void createDefaultsMissingStockToZero() {
    Warehouse request = new Warehouse();
    request.setBusinessUnitCode("WH-001");
    request.setLocation("AMSTERDAM-001");
    request.setCapacity(50);
    request.setStock(null);

    Warehouse response = resource.createANewWarehouseUnit(request);

    assertEquals(0, response.getStock());
  }

  @Test
  public void createTranslatesValidationFailureTo400() {
    doThrow(new IllegalArgumentException("capacity is required"))
        .when(createWarehouseOperation)
        .create(any());

    Warehouse request = new Warehouse();
    request.setBusinessUnitCode("WH-001");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createANewWarehouseUnit(request));

    assertEquals(400, ex.getResponse().getStatus());
  }

  // --- getAWarehouseUnitByID ---

  @Test
  public void getReturnsWarehouseWhenFound() {
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = "WH-001";
    domainWarehouse.location = "AMSTERDAM-001";
    domainWarehouse.capacity = 50;
    domainWarehouse.stock = 10;
    when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(domainWarehouse);

    Warehouse response = resource.getAWarehouseUnitByID("WH-001");

    assertEquals("WH-001", response.getBusinessUnitCode());
  }

  @Test
  public void getThrows404WhenNotFound() {
    when(warehouseRepository.findByBusinessUnitCode("MISSING")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAWarehouseUnitByID("MISSING"));

    assertEquals(404, ex.getResponse().getStatus());
  }

  // --- archiveAWarehouseUnitByID ---

  @Test
  public void archiveThrows404WhenNotFound() {
    when(warehouseRepository.findByBusinessUnitCode("MISSING")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("MISSING"));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void archiveTranslatesValidationFailureTo400() {
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = "WH-001";
    when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(domainWarehouse);
    doThrow(new IllegalArgumentException("already archived"))
        .when(archiveWarehouseOperation)
        .archive(any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("WH-001"));

    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void archiveDelegatesToUseCaseOnSuccess() {
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = "WH-001";
    when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(domainWarehouse);

    resource.archiveAWarehouseUnitByID("WH-001");

    verify(archiveWarehouseOperation).archive(domainWarehouse);
  }

  // --- replaceTheCurrentActiveWarehouse ---

  @Test
  public void replaceReturnsUpdatedWarehouseOnSuccess() {
    var updated = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    updated.businessUnitCode = "WH-001";
    updated.location = "ZWOLLE-001";
    updated.capacity = 30;
    updated.stock = 15;
    when(warehouseRepository.findByBusinessUnitCode("WH-001")).thenReturn(updated);

    Warehouse request = new Warehouse();
    request.setLocation("ZWOLLE-001");
    request.setCapacity(30);
    request.setStock(15);

    Warehouse response = resource.replaceTheCurrentActiveWarehouse("WH-001", request);

    assertEquals("ZWOLLE-001", response.getLocation());
    assertEquals(30, response.getCapacity());
  }

  @Test
  public void replaceTranslatesValidationFailureTo400() {
    doThrow(new IllegalArgumentException("does not exist"))
        .when(replaceWarehouseOperation)
        .replace(any());

    Warehouse request = new Warehouse();
    request.setLocation("ZWOLLE-001");
    request.setCapacity(30);
    request.setStock(15);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceTheCurrentActiveWarehouse("MISSING", request));

    assertEquals(400, ex.getResponse().getStatus());
  }

  // --- searchAndFilterWarehouseUnits ---

  @Test
  public void searchMapsResultsAndAppliesDefaults() {
    var found = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    found.businessUnitCode = "WH-001";
    found.location = "AMSTERDAM-001";
    found.capacity = 50;
    found.stock = 10;
    when(searchWarehousesOperation.search(any())).thenReturn(List.of(found));

    List<Warehouse> response =
        resource.searchAndFilterWarehouseUnits(
            "AMSTERDAM-001", null, null, "createdAt", "asc", null, null);

    assertEquals(1, response.size());
    assertEquals("WH-001", response.get(0).getBusinessUnitCode());
  }

  @Test
  public void searchConvertsBigIntegerQueryParams() {
    when(searchWarehousesOperation.search(any())).thenReturn(List.of());

    resource.searchAndFilterWarehouseUnits(
        null, BigInteger.valueOf(10), BigInteger.valueOf(90), "capacity", "desc", BigInteger.ONE, BigInteger.TEN);

    verify(searchWarehousesOperation)
        .search(
            argThatCriteria(
                c ->
                    c.minCapacity() == 10
                        && c.maxCapacity() == 90
                        && c.page() == 1
                        && c.pageSize() == 10));
  }

  @Test
  public void searchTranslatesValidationFailureTo400() {
    doThrow(new IllegalArgumentException("minCapacity cannot be greater than maxCapacity"))
        .when(searchWarehousesOperation)
        .search(any());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.searchAndFilterWarehouseUnits(
                    null, BigInteger.valueOf(90), BigInteger.TEN, "createdAt", "asc", null, null));

    assertEquals(400, ex.getResponse().getStatus());
  }

  private static com.fulfilment.application.monolith.warehouses.domain.models.WarehouseSearchCriteria
      argThatCriteria(
          java.util.function.Predicate<
                  com.fulfilment.application.monolith.warehouses.domain.models.WarehouseSearchCriteria>
              predicate) {
    return org.mockito.ArgumentMatchers.argThat(predicate::test);
  }
}
