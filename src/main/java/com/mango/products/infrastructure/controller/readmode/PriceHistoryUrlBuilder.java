package com.mango.products.infrastructure.controller.readmode;

import com.mango.products.infrastructure.repository.CursorDirection;
import com.mango.products.infrastructure.repository.SortOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class PriceHistoryUrlBuilder {

  public String buildUrl(
      String productId,
      String currency,
      LocalDate cursorDate,
      int pageSize,
      SortOrder sortOrder,
      CursorDirection cursorDirection) {
    if (cursorDate == null) {
      return null;
    }
    return "/products/"
        + productId
        + "/prices?currency="
        + currency
        + "&cursor="
        + encodeCursor(cursorDate)
        + "&cursorDirection="
        + cursorDirection.name()
        + "&pageSize="
        + pageSize
        + "&sortOrder="
        + sortOrder.name();
  }

  public String encodeCursor(LocalDate date) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(date.toString().getBytes(StandardCharsets.UTF_8));
  }
}
