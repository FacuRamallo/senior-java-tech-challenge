package com.mango.products.domain;

public class Product {
  private final Id id;
  private final Name name;
  private final Description description;

  private Product(Id id, Name name, Description description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public static Product create(Id id, Name name, Description description) {
    return new Product(id, name, description);
  }

  public static Product create(Name name, Description description) {
    return new Product(Id.generate(), name, description);
  }

  public Id getId() {
    return id;
  }

  public Name getName() {
    return name;
  }

  public Description getDescription() {
    return description;
  }
}
