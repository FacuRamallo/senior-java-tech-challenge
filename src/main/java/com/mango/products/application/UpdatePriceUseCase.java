package com.mango.products.application;

import com.mango.products.domain.DomainException.InactivePriceUpdateException;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

public class UpdatePriceUseCase {

  private final PriceRepository priceRepository;
  private final Clock clock;

  public UpdatePriceUseCase(PriceRepository priceRepository, Clock clock) {
    this.priceRepository = priceRepository;
    this.clock = clock;
  }

  public Optional<Price> execute(UpdatePriceCommand command) {
    var priceId = Id.fromString(command.priceId());
    var productId = Id.fromString(command.productId());
    var money = Money.from(command.amount(), command.currency());
    var validityPeriod = new ValidityPeriod(command.initDate(), command.endDate());

    var existingPrice = priceRepository.findById(priceId, productId);
    if (existingPrice.isEmpty()) {
      return Optional.empty();
    }

    if (!existingPrice.get().getValidityPeriod().isActive(LocalDate.now(clock))) {
      throw new InactivePriceUpdateException();
    }

    var price = Price.create(priceId, productId, money, validityPeriod);
    priceRepository.update(price);
    return Optional.of(price);
  }
}
