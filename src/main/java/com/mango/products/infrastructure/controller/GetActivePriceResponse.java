package com.mango.products.infrastructure.controller;

import com.mango.products.domain.Price;
import java.math.BigDecimal;

public record GetActivePriceResponse(BigDecimal value, String currency) {

  public static GetActivePriceResponse from(Price price) {
    return new GetActivePriceResponse(
        price.getMoney().amount(), price.getMoney().currency().value());
  }
}
