package com.mango.products.domain;

public class DuplicateProductNameException extends RuntimeException {

  private final Id conflictingProductId;
  private final Name name;

  public DuplicateProductNameException(Id conflictingProductId, Name name) {
    super("A product with the name '" + name.value() + "' already exists");
    this.conflictingProductId = conflictingProductId;
    this.name = name;
  }

  public DuplicateProductNameException(Name name) {
    this(null, name);
  }

  public Id conflictingProductId() {
    return conflictingProductId;
  }

  public Name name() {
    return name;
  }
}
