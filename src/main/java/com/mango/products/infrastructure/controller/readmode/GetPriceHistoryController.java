package com.mango.products.infrastructure.controller.readmode;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.ValidityPeriod;
import com.mango.products.infrastructure.repository.readmode.PaginationSortingStrategyFactory;
import com.mango.products.infrastructure.repository.readmode.PriceHistoryReader;
import com.mango.products.infrastructure.repository.readmode.SortOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetPriceHistoryController {

  private final PriceHistoryReader priceHistoryReader;
  private final PriceHistoryResponseAssembler assembler;

  public GetPriceHistoryController(
      PriceHistoryReader priceHistoryReader, PriceHistoryResponseAssembler assembler) {
    this.priceHistoryReader = priceHistoryReader;
    this.assembler = assembler;
  }

  @GetMapping(value = "/products/{id}/prices", params = "!date")
  public ResponseEntity<GetPriceHistoryResponse> getPriceHistory(
      @PathVariable("id") String rawProductId,
      @RequestParam(value = "currency", required = false) String rawCurrency,
      @RequestParam(value = "sortOrder", required = false) String rawSortOrder,
      @RequestParam(value = "cursor", required = false) String rawCursor,
      @RequestParam(value = "cursorDirection", required = false) String rawCursorDirection,
      @RequestParam(value = "pageSize", required = false) Integer rawPageSize) {
    var productId = Id.fromString(rawProductId);
    var currency = Currency.from(rawCurrency);
    var sortOrder = SortOrder.from(rawSortOrder);
    var pageSize = new PageSize(rawPageSize);

    LocalDate cursor = decodeCursor(rawCursor);
    var strategy =
        PaginationSortingStrategyFactory.create(
            rawCursorDirection, sortOrder, cursor, pageSize.lookAheadLimit());

    var lookAheadResults = priceHistoryReader.find(productId, currency, strategy);

    boolean wasCursorProvided = rawCursor != null && !rawCursor.isBlank();
    return ResponseEntity.ok(
        assembler.assemble(
            lookAheadResults,
            pageSize,
            sortOrder,
            strategy.getDirection(),
            wasCursorProvided,
            productId,
            currency));
  }

  private LocalDate decodeCursor(String rawCursor) {
    if (rawCursor == null || rawCursor.isBlank()) {
      return null;
    }
    try {
      byte[] decodedBytes = Base64.getUrlDecoder().decode(rawCursor.trim());
      String dateString = new String(decodedBytes, StandardCharsets.UTF_8);
      return ValidityPeriod.parseDate(dateString);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid cursor token", ex);
    }
  }
}
