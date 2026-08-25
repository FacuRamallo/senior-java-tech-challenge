package com.mango.products.domain;

import java.util.UUID;

public record Id(UUID value) {

  private static final int UUID_VERSION_7 = 7;

  public Id {
    validateVersion(value);
  }

  public static Id fromString(String raw) {
    validateNotBlank(raw);
    return new Id(parseUuid(raw));
  }

  private static void validateNotBlank(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Id must not be blank");
    }
  }

  private static UUID parseUuid(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Id must be a valid UUIDv7");
    }
  }

  private static void validateVersion(UUID value) {
    if (value == null || value.version() != UUID_VERSION_7) {
      throw new IllegalArgumentException("Id must be a valid UUIDv7");
    }
  }
}
