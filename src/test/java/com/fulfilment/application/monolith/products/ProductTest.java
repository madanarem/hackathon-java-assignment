package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ProductTest {

  @Test
  public void noArgConstructorLeavesFieldsUnset() {
    Product product = new Product();

    assertNull(product.id);
    assertNull(product.name);
  }

  @Test
  public void nameConstructorSetsName() {
    Product product = new Product("TONSTAD");

    assertEquals("TONSTAD", product.name);
  }
}
