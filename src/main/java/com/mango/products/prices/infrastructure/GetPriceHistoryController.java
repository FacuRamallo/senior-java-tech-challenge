package com.mango.products.prices.infrastructure;

import com.mango.products.prices.application.GetPriceHistoryQuery;
import com.mango.products.prices.application.GetPriceHistoryUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class GetPriceHistoryController {

  private final GetPriceHistoryUseCase getPriceHistoryUseCase;

  public GetPriceHistoryController(GetPriceHistoryUseCase getPriceHistoryUseCase) {
    this.getPriceHistoryUseCase = getPriceHistoryUseCase;
  }

  @GetMapping(value = "/{id}/prices", params = "!date")
  public ResponseEntity<List<GetPriceHistoryResponse>> getPriceHistory(
      @PathVariable("id") String productId,
      @RequestParam(value = "currency", required = false) String currency) {
    var query = new GetPriceHistoryQuery(productId, currency);
    var prices =
        getPriceHistoryUseCase.execute(query).stream().map(GetPriceHistoryResponse::from).toList();
    return ResponseEntity.ok(prices);
  }
}
