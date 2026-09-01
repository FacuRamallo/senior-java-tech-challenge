package com.mango.products.domain;

import com.mango.products.domain.DomainException.BlankIdException;
import com.mango.products.domain.DomainException.InvalidUuidV7Exception;
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
      throw new BlankIdException();
    }
  }

  private static UUID parseUuid(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (Exception ex) {
      throw new InvalidUuidV7Exception();
    }
  }

  private static void validateVersion(UUID value) {
    if (value == null || value.version() != UUID_VERSION_7) {
      throw new InvalidUuidV7Exception();
    }
  }
}
