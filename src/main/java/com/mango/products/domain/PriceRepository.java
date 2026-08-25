package com.mango.products.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository {

  void save(Price price);

  Optional<Price> findActivePrice(Id productId, LocalDate date, Currency currency);

  List<Price> findPriceHistory(Id productId, Currency currency);
}
