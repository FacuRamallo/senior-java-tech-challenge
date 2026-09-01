package com.mango.products.domain;

import com.mango.products.domain.DomainException.InvalidCurrencyCodeException;

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
      throw new InvalidCurrencyCodeException();
    }
  }

  public int fractionDigits() {
    return java.util.Currency.getInstance(value).getDefaultFractionDigits();
  }
}
