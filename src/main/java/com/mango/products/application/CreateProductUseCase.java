package com.mango.products.application;

import com.mango.products.domain.Description;
import com.mango.products.domain.Id;
import com.mango.products.domain.IdGenerator;
import com.mango.products.domain.Name;
import com.mango.products.domain.Product;
import com.mango.products.domain.ProductRepository;

public class CreateProductUseCase {
  private final ProductRepository productRepository;
  private final IdGenerator idGenerator;

  public CreateProductUseCase(ProductRepository productRepository, IdGenerator idGenerator) {
    this.productRepository = productRepository;
    this.idGenerator = idGenerator;
  }

  public Product execute(CreateProductCommand command) {
    var id =
        (command.id() != null && !command.id().isBlank())
            ? Id.fromString(command.id())
            : idGenerator.nextIdentity();
    var name = new Name(command.name());
    var description = new Description(command.description());
    var product = Product.create(id, name, description);
    productRepository.save(product);
    return product;
  }
}
