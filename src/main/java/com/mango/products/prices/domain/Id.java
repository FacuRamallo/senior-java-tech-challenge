package com.mango.products.prices.domain;

import java.util.Objects;
import java.util.UUID;

public record Id(UUID value) {

  public Id {
    Objects.requireNonNull(value, "Id must not be null");
    validateVersion(value);
  }

  public static Id fromString(String raw) {
    Objects.requireNonNull(raw, "Id must not be null");
    validateNotBlank(raw);
    return new Id(UUID.fromString(raw));
  }

  private static void validateNotBlank(String value) {
    if (value.isBlank()) throw new IllegalArgumentException("Id must not be blank");
  }

  private static void validateVersion(UUID value) {
    if (value.version() != 7) throw new IllegalArgumentException("Id must be a valid UUIDv7");
  }
}
