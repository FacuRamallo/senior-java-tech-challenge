package com.mango.products.prices.application;

import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Money;
import com.mango.products.prices.domain.Price;
import com.mango.products.prices.domain.PriceRepository;
import com.mango.products.prices.domain.ValidityPeriod;

public class AddPriceToProductUseCase {

  private final PriceRepository priceRepository;

  public AddPriceToProductUseCase(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  public void execute(AddPriceToProductCommand command) {
    var priceId = Id.fromString(command.priceId());
    var productId = Id.fromString(command.productId());
    var money = Money.from(command.amount(), command.currency());
    var validityPeriod = new ValidityPeriod(command.initDate(), command.endDate());
    var price = Price.create(priceId, productId, money, validityPeriod);
    priceRepository.save(price);
  }
}
