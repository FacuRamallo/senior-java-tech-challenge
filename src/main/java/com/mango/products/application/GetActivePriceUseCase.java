package com.mango.products.application;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.util.Optional;

public class GetActivePriceUseCase {

  private final PriceRepository priceRepository;

  public GetActivePriceUseCase(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  public Optional<Price> execute(GetActivePriceQuery query) {
    var productId = Id.fromString(query.productId());
    var date = ValidityPeriod.parseDate(query.date());
    var currency = Currency.from(query.currency());
    return priceRepository.findActivePrice(productId, date, currency);
  }
}
