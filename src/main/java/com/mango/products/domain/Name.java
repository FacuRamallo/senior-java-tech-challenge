package com.mango.products.domain;

import com.mango.products.domain.DomainException.BlankNameException;

public record Name(String value) {

  public Name {
    validate(value);
    value = value.trim();
  }

  private static void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new BlankNameException();
    }
  }
}
