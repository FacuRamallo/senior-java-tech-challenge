package com.mango.products.application;

import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.util.Optional;

public class UpdatePriceUseCase {

  private final PriceRepository priceRepository;

  public UpdatePriceUseCase(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  public Optional<Price> execute(UpdatePriceCommand command) {
    var priceId = Id.fromString(command.priceId());
    var productId = Id.fromString(command.productId());
    var money = Money.from(command.amount(), command.currency());
    var validityPeriod = new ValidityPeriod(command.initDate(), command.endDate());
    var price = Price.create(priceId, productId, money, validityPeriod);
    boolean updated = priceRepository.update(price);
    if (!updated) {
      return Optional.empty();
    }
    return Optional.of(price);
  }
}
