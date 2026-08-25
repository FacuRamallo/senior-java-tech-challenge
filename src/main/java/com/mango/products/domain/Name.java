package com.mango.products.domain;

public record Name(String value) {

  public Name {
    validate(value);
    value = value.trim();
  }

  private static void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Name cannot be blank");
    }
  }
}
