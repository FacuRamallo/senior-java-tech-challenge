package com.mango.products.application;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
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
