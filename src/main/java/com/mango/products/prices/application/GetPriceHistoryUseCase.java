package com.mango.products.prices.application;

import com.mango.products.prices.domain.Currency;
import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Price;
import com.mango.products.prices.domain.PriceRepository;
import java.util.List;

public class GetPriceHistoryUseCase {

  private final PriceRepository priceRepository;

  public GetPriceHistoryUseCase(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  public List<Price> execute(GetPriceHistoryQuery query) {
    var productId = Id.fromString(query.productId());
    var currency = Currency.from(query.currency());
    return priceRepository.findPriceHistory(productId, currency);
  }
}
