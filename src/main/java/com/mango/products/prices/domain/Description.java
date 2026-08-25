package com.mango.products.prices.domain;

public record Description(String value) {

  public Description {
    validate(value);
    value = value.trim();
  }

  private static void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Description cannot be blank");
    }
  }
}
