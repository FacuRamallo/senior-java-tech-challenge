package com.mango.products.infrastructure;

import com.fasterxml.uuid.Generators;
import com.mango.products.application.AddPriceToProductCommand;
import com.mango.products.application.AddPriceToProductUseCase;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class AddPriceController {

  private final AddPriceToProductUseCase addPriceToProductUseCase;

  public AddPriceController(AddPriceToProductUseCase addPriceToProductUseCase) {
    this.addPriceToProductUseCase = addPriceToProductUseCase;
  }

  @PostMapping("/{id}/prices")
  public ResponseEntity<Void> addPrice(
      @PathVariable("id") String productId, @RequestBody AddPriceRequest request) {
    var priceId = Generators.timeBasedEpochGenerator().generate().toString();
    var command =
        new AddPriceToProductCommand(
            priceId,
            productId,
            request.value(),
            request.currency(),
            request.initDate(),
            request.endDate());
    addPriceToProductUseCase.execute(command);
    URI location = URI.create("/products/" + productId + "/prices/" + priceId);
    return ResponseEntity.created(location).build();
  }
}
