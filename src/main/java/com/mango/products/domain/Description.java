package com.mango.products.domain;

import com.mango.products.domain.DomainException.BlankDescriptionException;

public record Description(String value) {

  public Description {
    validate(value);
    value = value.trim();
  }

  private static void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new BlankDescriptionException();
    }
  }
}
