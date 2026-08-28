package com.mango.products.application;

import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;

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

    var latestPrice = priceRepository.findLatestPrice(productId, money.currency());
    if (latestPrice.isPresent()) {
      var latestPriceValidityPeriod = latestPrice.get().getValidityPeriod();
      if (latestPriceValidityPeriod.hasOpenEndedEndDate()) {
        throw new IllegalArgumentException(
            "Cannot create a new price while the current price has an open-ended end date");
      }
      if (!validityPeriod.initDate().isAfter(latestPriceValidityPeriod.endDate())) {
        throw new IllegalArgumentException(
            "New price init date must be after the last price end date");
      }
    }

    var price = Price.create(priceId, productId, money, validityPeriod);
    priceRepository.save(price);
  }
}
