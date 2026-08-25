package com.mango.products.prices.domain;

import java.time.LocalDate;

public record ValidityPeriod(LocalDate initDate, LocalDate endDate) {

  public ValidityPeriod {
    validateDates(initDate, endDate);
  }

  private static void validateDates(LocalDate initDate, LocalDate endDate) {
    if (initDate == null) {
      throw new IllegalArgumentException("Init date must not be null");
    }
    if (endDate != null && !initDate.isBefore(endDate)) {
      throw new IllegalArgumentException("Init date must be before end date");
    }
  }
}
