package com.mango.products.infrastructure.controller;

import com.mango.products.application.DeletePriceCommand;
import com.mango.products.application.DeletePriceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeletePriceController {

  private final DeletePriceUseCase deletePriceUseCase;

  public DeletePriceController(DeletePriceUseCase deletePriceUseCase) {
    this.deletePriceUseCase = deletePriceUseCase;
  }

  @DeleteMapping("/products/{id}/prices/{priceId}")
  public ResponseEntity<Void> deletePrice(
      @PathVariable("id") String productId, @PathVariable("priceId") String priceId) {
    var command = new DeletePriceCommand(priceId, productId);
    boolean deleted = deletePriceUseCase.execute(command);
    if (!deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
