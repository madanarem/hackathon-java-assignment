package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class StoreTest {

  @Test
  public void noArgConstructorLeavesFieldsUnset() {
    Store store = new Store();

    assertNull(store.id);
    assertNull(store.name);
  }

  @Test
  public void nameConstructorSetsName() {
    Store store = new Store("TONSTAD");

    assertEquals("TONSTAD", store.name);
  }
}
