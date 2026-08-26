package com.mango.products.application;

import com.mango.products.domain.Id;
import com.mango.products.domain.PriceRepository;

public class DeletePriceUseCase {

  private final PriceRepository priceRepository;

  public DeletePriceUseCase(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  public boolean execute(DeletePriceCommand command) {
    var priceId = Id.fromString(command.priceId());
    var productId = Id.fromString(command.productId());
    return priceRepository.deleteById(priceId, productId);
  }
}
