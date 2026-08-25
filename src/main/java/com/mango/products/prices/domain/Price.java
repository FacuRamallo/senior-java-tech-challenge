package com.mango.products.prices.domain;

public class Price {

  private final Id id;
  private final Id productId;
  private final Money money;
  private final ValidityPeriod validityPeriod;

  private Price(Id id, Id productId, Money money, ValidityPeriod validityPeriod) {
    this.id = id;
    this.productId = productId;
    this.money = money;
    this.validityPeriod = validityPeriod;
  }

  public static Price create(Id id, Id productId, Money money, ValidityPeriod validityPeriod) {
    return new Price(id, productId, money, validityPeriod);
  }

  public Id getId() {
    return id;
  }

  public Id getProductId() {
    return productId;
  }

  public Money getMoney() {
    return money;
  }

  public ValidityPeriod getValidityPeriod() {
    return validityPeriod;
  }
}
