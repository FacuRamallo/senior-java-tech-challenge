package com.mango.products.application;

import com.mango.products.domain.Description;
import com.mango.products.domain.Id;
import com.mango.products.domain.Name;
import com.mango.products.domain.Product;
import com.mango.products.domain.ProductRepository;

public class CreateProductUseCase {
  private final ProductRepository productRepository;

  public CreateProductUseCase(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public Product execute(CreateProductCommand command) {
    var id = Id.fromStringOrGenerate(command.id());
    var name = new Name(command.name());
    var description = new Description(command.description());
    var product = Product.create(id, name, description);
    productRepository.save(product);
    return product;
  }
}
