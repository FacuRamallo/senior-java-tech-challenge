package com.mango.products.prices.domain;

import java.util.Objects;

public record Name(String value) {

  public Name {
    Objects.requireNonNull(value);
    if (value.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
    value = value.trim();
  }
}
