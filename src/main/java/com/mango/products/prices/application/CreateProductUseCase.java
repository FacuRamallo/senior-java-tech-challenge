package com.mango.products.prices.application;

import com.mango.products.prices.domain.Description;
import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Name;
import com.mango.products.prices.domain.Product;
import com.mango.products.prices.domain.ProductRepository;

public class CreateProductUseCase {
  private final ProductRepository productRepository;

  public CreateProductUseCase(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public void execute(CreateProductCommand command) {
    var id = Id.fromString(command.id());
    var name = new Name(command.name());
    var description = new Description(command.description());
    var product = Product.create(id, name, description);
    productRepository.save(product);
  }
}
