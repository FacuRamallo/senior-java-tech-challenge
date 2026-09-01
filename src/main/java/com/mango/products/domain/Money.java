package com.mango.products.domain;

import com.mango.products.domain.DomainException.AmountMustBePositiveException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, Currency currency) {

  public Money {
    if (currency == null) {
      currency = Currency.DEFAULT;
    }
    validateAmount(amount);
    amount = amount.setScale(currency.fractionDigits(), RoundingMode.HALF_UP);
  }

  public static Money from(BigDecimal amount, String rawCurrency) {
    return new Money(amount, Currency.from(rawCurrency));
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AmountMustBePositiveException();
    }
  }
}
