package com.mango.products.prices.infrastructure;

import com.mango.products.prices.application.CreateProductCommand;
import com.mango.products.prices.application.CreateProductUseCase;
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
  public ResponseEntity<Void> createProduct(@RequestBody CreateProductRequest request) {
    var command = new CreateProductCommand(request.id(), request.name(), request.description());
    createProductUseCase.execute(command);
    URI location = URI.create("/products/" + request.id());
    return ResponseEntity.created(location).build();
  }
}
