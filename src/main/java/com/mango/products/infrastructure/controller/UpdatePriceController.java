package com.mango.products.infrastructure.controller;

import com.mango.products.application.UpdatePriceCommand;
import com.mango.products.application.UpdatePriceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpdatePriceController {

  private final UpdatePriceUseCase updatePriceUseCase;

  public UpdatePriceController(UpdatePriceUseCase updatePriceUseCase) {
    this.updatePriceUseCase = updatePriceUseCase;
  }

  @PutMapping("/products/{id}/prices/{priceId}")
  public ResponseEntity<UpdatePriceResponse> updatePrice(
      @PathVariable("id") String productId,
      @PathVariable("priceId") String priceId,
      @RequestBody UpdatePriceRequest request) {
    var command =
        new UpdatePriceCommand(
            priceId,
            productId,
            request.value(),
            request.currency(),
            request.initDate(),
            request.endDate());
    return updatePriceUseCase
        .execute(command)
        .map(price -> ResponseEntity.ok(UpdatePriceResponse.from(price)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
