package com.mango.products.domain;

public record Currency(String value) {

  public static final String DEFAULT_CURRENCY_CODE = "EUR";
  public static final Currency DEFAULT = new Currency(DEFAULT_CURRENCY_CODE);

  public Currency {
    if (value == null || value.isBlank()) {
      value = DEFAULT_CURRENCY_CODE;
    } else {
      value = validateAndFormat(value);
    }
  }

  public static Currency from(String rawCurrency) {
    return new Currency(rawCurrency);
  }

  private static String validateAndFormat(String rawCurrency) {
    try {
      return java.util.Currency.getInstance(rawCurrency.trim().toUpperCase()).getCurrencyCode();
    } catch (Exception ex) {
      throw new IllegalArgumentException("Currency must be a valid ISO-4217 code");
    }
  }

  public int fractionDigits() {
    return java.util.Currency.getInstance(value).getDefaultFractionDigits();
  }
}
