package com.mango.products.infrastructure;

import com.mango.products.application.CreateProductCommand;
import com.mango.products.application.CreateProductUseCase;
import com.mango.products.domain.Product;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class CreateProductController {

  private final CreateProductUseCase createProductUseCase;

  public CreateProductController(CreateProductUseCase createProductUseCase) {
    this.createProductUseCase = createProductUseCase;
  }

  @PostMapping
  public ResponseEntity<CreateProductResponse> createProduct(
      @RequestBody CreateProductRequest request) {
    var command = new CreateProductCommand(request.id(), request.name(), request.description());
    Product product = createProductUseCase.execute(command);
    URI location = URI.create("/products/" + product.getId().value());
    return ResponseEntity.created(location).body(CreateProductResponse.from(product));
  }
}
