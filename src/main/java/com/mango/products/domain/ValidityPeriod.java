package com.mango.products.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record ValidityPeriod(LocalDate initDate, LocalDate endDate) {

  public ValidityPeriod {
    if (initDate == null) {
      throw new IllegalArgumentException("Init date must not be null");
    }
    if (endDate != null && !initDate.isBefore(endDate)) {
      throw new IllegalArgumentException("Init date must be before end date");
    }
  }

  public static LocalDate parseDate(String rawDate) {
    if (rawDate == null || rawDate.isBlank()) {
      throw new IllegalArgumentException("Date must not be blank");
    }
    try {
      return LocalDate.parse(rawDate.trim());
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Date must be in ISO-8601 format (YYYY-MM-DD)");
    }
  }

  public static ValidityPeriod from(String rawInitDate, String rawEndDate) {
    LocalDate init = parseDate(rawInitDate);
    LocalDate end = (rawEndDate != null && !rawEndDate.isBlank()) ? parseDate(rawEndDate) : null;
    return new ValidityPeriod(init, end);
  }

  public boolean contains(LocalDate date) {
    if (date == null) {
      return false;
    }
    boolean afterOrEqualInit = !date.isBefore(initDate);
    boolean beforeOrEqualEnd = endDate == null || !date.isAfter(endDate);
    return afterOrEqualInit && beforeOrEqualEnd;
  }
}
