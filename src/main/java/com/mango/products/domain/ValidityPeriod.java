package com.mango.products.domain;

import com.mango.products.domain.DomainException.BlankDateException;
import com.mango.products.domain.DomainException.InitDateNotBeforeEndDateException;
import com.mango.products.domain.DomainException.InvalidDateFormatException;
import com.mango.products.domain.DomainException.NullInitDateException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record ValidityPeriod(LocalDate initDate, LocalDate endDate) {

  public ValidityPeriod {
    if (initDate == null) {
      throw new NullInitDateException();
    }
    if (endDate != null && !initDate.isBefore(endDate)) {
      throw new InitDateNotBeforeEndDateException();
    }
  }

  public static LocalDate parseDate(String rawDate) {
    if (rawDate == null || rawDate.isBlank()) {
      throw new BlankDateException();
    }
    try {
      return LocalDate.parse(rawDate.trim());
    } catch (DateTimeParseException ex) {
      throw new InvalidDateFormatException();
    }
  }

  public boolean isActive(LocalDate date) {
    if (date == null) {
      return false;
    }
    boolean afterOrEqualInit = !date.isBefore(initDate);
    boolean beforeOrEqualEnd = endDate == null || !date.isAfter(endDate);
    return afterOrEqualInit && beforeOrEqualEnd;
  }

  public boolean hasOpenEndedEndDate() {
    return endDate == null;
  }
}
