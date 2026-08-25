package com.fulfilment.application.monolith.stores;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit test for {@link StoreEventObserver}, with {@link LegacyStoreManagerGateway}
 * mocked. See {@link StoreEventObserverTest} for the corresponding @QuarkusTest-based
 * coverage of the same behavior.
 */
@ExtendWith(MockitoExtension.class)
public class StoreEventObserverUnitTest {

  @Mock private LegacyStoreManagerGateway legacyStoreManagerGateway;

  private StoreEventObserver observer;

  @BeforeEach
  public void setup() throws Exception {
    observer = new StoreEventObserver();
    var field = StoreEventObserver.class.getDeclaredField("legacyStoreManagerGateway");
    field.setAccessible(true);
    field.set(observer, legacyStoreManagerGateway);
  }

  @Test
  public void onStoreCreatedDelegatesToLegacyGateway() {
    Store store = new Store("Test Store");

    observer.onStoreCreated(new StoreCreatedEvent(store));

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(store);
  }

  @Test
  public void onStoreUpdatedDelegatesToLegacyGateway() {
    Store store = new Store("Test Store");

    observer.onStoreUpdated(new StoreUpdatedEvent(store));

    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(store);
  }
}
