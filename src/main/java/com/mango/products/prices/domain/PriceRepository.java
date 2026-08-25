package com.mango.products.prices.domain;

import java.time.LocalDate;
import java.util.Optional;

public interface PriceRepository {

  void save(Price price);

  Optional<Price> findActivePrice(Id productId, LocalDate date, Currency currency);
}
