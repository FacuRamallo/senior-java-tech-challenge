package com.mango.products.infrastructure.controller.readmode;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.ValidityPeriod;
import com.mango.products.infrastructure.repository.readmode.ActivePriceReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetActivePriceController {

  private final ActivePriceReader activePriceReader;

  public GetActivePriceController(ActivePriceReader activePriceReader) {
    this.activePriceReader = activePriceReader;
  }

  @GetMapping(value = "/products/{id}/prices", params = "date")
  public ResponseEntity<GetActivePriceResponse> getActivePrice(
      @PathVariable("id") String rawProductId,
      @RequestParam("date") String rawDate,
      @RequestParam(value = "currency", required = false) String rawCurrency) {
    var productId = Id.fromString(rawProductId);
    var date = ValidityPeriod.parseDate(rawDate);
    var currency = Currency.from(rawCurrency);

    return activePriceReader
        .findActivePrice(productId, date, currency)
        .map(price -> ResponseEntity.ok(GetActivePriceResponse.from(price)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
