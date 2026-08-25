package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link LegacyStoreManagerGateway}. It has no injected
 * dependencies, so it can be exercised directly without a Quarkus/CDI context.
 */
public class LegacyStoreManagerGatewayTest {

  private final LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();

  @Test
  public void createStoreOnLegacySystemDoesNotThrowForValidStore() {
    Store store = new Store("Legacy Create Store");
    store.quantityProductsInStock = 5;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  public void updateStoreOnLegacySystemDoesNotThrowForValidStore() {
    Store store = new Store("Legacy Update Store");
    store.quantityProductsInStock = 12;

    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }

  @Test
  public void writeFailureIsSwallowedRatherThanPropagated() {
    // A name with characters illegal in a filename (e.g. path separators) makes
    // Files.createTempFile fail internally; the gateway must not let that propagate,
    // since a legacy-system sync failure should never break the caller's flow.
    Store store = new Store("in/valid\\name");
    store.quantityProductsInStock = 1;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }
}
