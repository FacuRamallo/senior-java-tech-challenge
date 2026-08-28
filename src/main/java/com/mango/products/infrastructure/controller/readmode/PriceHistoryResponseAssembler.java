package com.mango.products.infrastructure.controller.readmode;

import static com.mango.products.infrastructure.repository.CursorDirection.defaultCursorDirectionFor;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Price;
import com.mango.products.infrastructure.repository.CursorDirection;
import com.mango.products.infrastructure.repository.SortOrder;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PriceHistoryResponseAssembler {

  private final PriceHistoryUrlBuilder urlBuilder;

  public PriceHistoryResponseAssembler(PriceHistoryUrlBuilder urlBuilder) {
    this.urlBuilder = urlBuilder;
  }

  public GetPriceHistoryResponse assemble(
      List<Price> lookAheadResults,
      PageSize pageSize,
      SortOrder sortOrder,
      CursorDirection cursorDirection,
      boolean wasCursorProvided,
      Id productId,
      Currency currency) {
    boolean hasMore = lookAheadResults.size() > pageSize.value();
    var displayPrices =
        prepareDisplayPrices(lookAheadResults, pageSize, sortOrder, cursorDirection);

    String nextUrl =
        assembleNextUrl(
            displayPrices,
            hasMore,
            wasCursorProvided,
            sortOrder,
            cursorDirection,
            productId,
            currency,
            pageSize);

    String prevUrl =
        assemblePreviousUrl(
            displayPrices,
            hasMore,
            wasCursorProvided,
            sortOrder,
            cursorDirection,
            productId,
            currency,
            pageSize);

    return new GetPriceHistoryResponse(nextUrl, prevUrl, toPriceResponses(displayPrices));
  }

  private List<Price> prepareDisplayPrices(
      List<Price> lookAheadResults,
      PageSize pageSize,
      SortOrder sortOrder,
      CursorDirection cursorDirection) {
    boolean hasMore = lookAheadResults.size() > pageSize.value();
    var prices = hasMore ? lookAheadResults.subList(0, pageSize.value()) : lookAheadResults;
    return sortOrder.requiresReversal(cursorDirection) ? prices.reversed() : prices;
  }

  private String assembleNextUrl(
      List<Price> displayPrices,
      boolean hasMore,
      boolean wasCursorProvided,
      SortOrder sortOrder,
      CursorDirection cursorDirection,
      Id productId,
      Currency currency,
      PageSize pageSize) {
    boolean isForward = cursorDirection == defaultCursorDirectionFor(sortOrder);
    boolean hasNextPage = isForward ? hasMore : wasCursorProvided;
    LocalDate nextCursorDate =
        hasNextPage && !displayPrices.isEmpty()
            ? displayPrices.getLast().getValidityPeriod().initDate()
            : null;

    return urlBuilder.buildUrl(
        productId.value().toString(),
        currency.value(),
        nextCursorDate,
        pageSize.value(),
        sortOrder,
        defaultCursorDirectionFor(sortOrder));
  }

  private String assemblePreviousUrl(
      List<Price> displayPrices,
      boolean hasMore,
      boolean wasCursorProvided,
      SortOrder sortOrder,
      CursorDirection cursorDirection,
      Id productId,
      Currency currency,
      PageSize pageSize) {
    boolean isForward = cursorDirection == defaultCursorDirectionFor(sortOrder);
    boolean hasPreviousPage = isForward ? wasCursorProvided : hasMore;
    LocalDate prevCursorDate =
        hasPreviousPage && !displayPrices.isEmpty()
            ? displayPrices.getFirst().getValidityPeriod().initDate()
            : null;

    return urlBuilder.buildUrl(
        productId.value().toString(),
        currency.value(),
        prevCursorDate,
        pageSize.value(),
        sortOrder,
        defaultCursorDirectionFor(sortOrder).opposite());
  }

  private List<PriceHistoryItemResponse> toPriceResponses(List<Price> prices) {
    return prices.stream().map(PriceHistoryItemResponse::from).toList();
  }
}
