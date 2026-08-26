package com.mango.products.infrastructure.controller;

import com.mango.products.domain.Price;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePriceResponse(
    UUID id, BigDecimal value, String currency, LocalDate initDate, LocalDate endDate) {

  public static UpdatePriceResponse from(Price price) {
    return new UpdatePriceResponse(
        price.getId().value(),
        price.getMoney().amount(),
        price.getMoney().currency().value(),
        price.getValidityPeriod().initDate(),
        price.getValidityPeriod().endDate());
  }
}
