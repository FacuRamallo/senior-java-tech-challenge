package com.mango.products.domain;

import java.util.Optional;

public interface PriceRepository {

  void save(Price price);

  Optional<Price> findById(Id priceId, Id productId);

  boolean update(Price price);

  Optional<Price> findLatestPrice(Id productId, Currency currency);
}
