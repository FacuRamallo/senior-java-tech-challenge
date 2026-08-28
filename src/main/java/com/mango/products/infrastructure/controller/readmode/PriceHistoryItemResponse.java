package com.mango.products.infrastructure.controller.readmode;

import com.mango.products.domain.Price;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PriceHistoryItemResponse(
    UUID id, BigDecimal value, String currency, LocalDate initDate, LocalDate endDate) {

  public static PriceHistoryItemResponse from(Price price) {
    return new PriceHistoryItemResponse(
        price.getId().value(),
        price.getMoney().amount(),
        price.getMoney().currency().value(),
        price.getValidityPeriod().initDate(),
        price.getValidityPeriod().endDate());
  }
}
