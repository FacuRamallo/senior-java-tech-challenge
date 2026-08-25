package com.mango.products.infrastructure.controller;

import com.mango.products.domain.Product;

public record CreateProductResponse(String id, String name, String description) {

  public static CreateProductResponse from(Product product) {
    return new CreateProductResponse(
        product.getId().value().toString(),
        product.getName().value(),
        product.getDescription().value());
  }
}
