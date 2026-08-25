package com.mango.products.infrastructure.controller;

import com.mango.products.domain.Price;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GetPriceHistoryResponse(
    UUID id, BigDecimal value, String currency, LocalDate initDate, LocalDate endDate) {

  public static GetPriceHistoryResponse from(Price price) {
    return new GetPriceHistoryResponse(
        price.getId().value(),
        price.getMoney().amount(),
        price.getMoney().currency().value(),
        price.getValidityPeriod().initDate(),
        price.getValidityPeriod().endDate());
  }
}
