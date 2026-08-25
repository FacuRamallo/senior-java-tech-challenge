package com.mango.products.infrastructure;

import com.mango.products.application.GetActivePriceQuery;
import com.mango.products.application.GetActivePriceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class GetActivePriceController {

  private final GetActivePriceUseCase getActivePriceUseCase;

  public GetActivePriceController(GetActivePriceUseCase getActivePriceUseCase) {
    this.getActivePriceUseCase = getActivePriceUseCase;
  }

  @GetMapping(value = "/{id}/prices", params = "date")
  public ResponseEntity<GetActivePriceResponse> getActivePrice(
      @PathVariable("id") String productId,
      @RequestParam("date") String date,
      @RequestParam(value = "currency", required = false) String currency) {
    var query = new GetActivePriceQuery(productId, date, currency);
    return getActivePriceUseCase
        .execute(query)
        .map(price -> ResponseEntity.ok(GetActivePriceResponse.from(price)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
