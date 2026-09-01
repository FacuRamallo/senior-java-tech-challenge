package com.mango.products.domain;

import java.util.Optional;

public interface ProductRepository {
  void save(Product product);

  Optional<Id> findConflictingProductId(Name name);
}
