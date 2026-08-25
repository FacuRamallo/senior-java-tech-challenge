package com.mango.products.prices.domain;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Currency.getInstance;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

  public static final String EURO = "EUR";
  private static final Currency DEFAULT_CURRENCY = getInstance(EURO);

  public Money {
    if (currency == null) {
      currency = DEFAULT_CURRENCY;
    }
    validateAmount(amount);
    amount = amount.setScale(currency.getDefaultFractionDigits(), HALF_UP);
  }

  public static Money from(BigDecimal amount, String rawCurrency) {
    return new Money(amount, parseCurrency(rawCurrency));
  }

  private static Currency parseCurrency(String rawCurrency) {
    if (rawCurrency == null || rawCurrency.isBlank()) {
      return DEFAULT_CURRENCY;
    }
    try {
      return getInstance(rawCurrency.trim().toUpperCase());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Currency must be a valid ISO-4217 code");
    }
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be greater than zero");
    }
  }
}
