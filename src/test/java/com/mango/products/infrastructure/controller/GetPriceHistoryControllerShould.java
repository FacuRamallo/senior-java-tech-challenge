package com.mango.products.infrastructure.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.ValidityPeriod;
import com.mango.products.infrastructure.controller.readmode.GetPriceHistoryController;
import com.mango.products.infrastructure.controller.readmode.PriceHistoryItemResponse;
import com.mango.products.infrastructure.controller.readmode.PriceHistoryResponseAssembler;
import com.mango.products.infrastructure.controller.readmode.PriceHistoryUrlBuilder;
import com.mango.products.infrastructure.repository.PaginationStrategy;
import com.mango.products.infrastructure.repository.PriceHistoryReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPriceHistoryControllerShould {

  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String PRICE_ID_1 = "01952e42-7a57-7000-8000-000000000002";
  private static final String PRICE_ID_2 = "01952e42-7a57-7000-8000-000000000003";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final String EUR = "EUR";
  private static final String USD = "USD";
  private static final String ASC = "ASC";
  private static final String DESC = "DESC";
  private static final String FUTURE = "FUTURE";
  private static final String PAST = "PAST";
  private static final LocalDate BASE_DATE = LocalDate.of(2024, 1, 1);
  private static final BigDecimal AMOUNT = new BigDecimal("99.99");

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_ID_BLANK = "Id must not be blank";
  private static final String ERROR_CURRENCY_ISO = "Currency must be a valid ISO-4217 code";
  private static final String ERROR_SORT_ORDER = "Sort order must be one of: ASC, DESC";
  private static final String ERROR_CURSOR_DIRECTION =
      "Cursor direction must be one of: FUTURE, PAST";
  private static final String ERROR_CURSOR_TOKEN = "Invalid cursor token";
  private static final String ERROR_PAGE_SIZE = "Page size must not exceed 100";

  @Mock private PriceHistoryReader priceHistoryReader;

  private GetPriceHistoryController controller;

  @BeforeEach
  void setUp() {
    var assembler = new PriceHistoryResponseAssembler(new PriceHistoryUrlBuilder());
    controller = new GetPriceHistoryController(priceHistoryReader, assembler);
  }

  @Test
  void returnPriceHistoryForProductWithExplicitCurrencyAndSortOrder() {
    var price1 = aPrice(PRICE_ID_1, USD, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    var price2 = aPrice(PRICE_ID_2, USD, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28));
    when(priceHistoryReader.find(
            eq(Id.fromString(PRODUCT_ID)), eq(Currency.from(USD)), any(PaginationStrategy.class)))
        .thenReturn(List.of(price1, price2));

    var response = controller.getPriceHistory(PRODUCT_ID, USD, ASC, null, null, 50);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices()).hasSize(2);
    assertThat(response.getBody().prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.naturalOrder())
        .containsExactly(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void returnPriceHistoryWithDefaultCurrencyAndSortOrderWhenOmittedOrBlank(String blankValue) {
    var price1 = aPrice(PRICE_ID_1, EUR, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28));
    var price2 = aPrice(PRICE_ID_2, EUR, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    when(priceHistoryReader.find(
            eq(Id.fromString(PRODUCT_ID)), eq(Currency.DEFAULT), any(PaginationStrategy.class)))
        .thenReturn(List.of(price1, price2));

    var response = controller.getPriceHistory(PRODUCT_ID, blankValue, blankValue, null, null, null);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices()).hasSize(2);
    assertThat(response.getBody().prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder())
        .containsExactly(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1));
  }

  @Test
  void returnNextUrlAndSliceToPageSizeWhenLookAheadResultsExceedPageSize() {
    var lookAheadResults = pricesDescending(0, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(lookAheadResults);

    var response = controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, null, 20);

    var expectedCursor =
        encodeCursor(lookAheadResults.get(19).getValidityPeriod().initDate().toString());

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices()).hasSize(20);
    assertThat(response.getBody().prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(response.getBody().next())
        .isEqualTo(
            "/products/"
                + PRODUCT_ID
                + "/prices?currency=EUR&cursor="
                + expectedCursor
                + "&cursorDirection=PAST&pageSize=20&sortOrder=DESC");
    assertThat(response.getBody().previous()).isNull();
  }

  @Test
  void returnNullNextUrlAndAllResultsWhenLookAheadDoesNotExceedPageSize() {
    var lookAheadResults = pricesDescending(0, 5);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(lookAheadResults);

    var response = controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, null, 20);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices()).hasSize(5);
    assertThat(response.getBody().prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(response.getBody().next()).isNull();
    assertThat(response.getBody().previous()).isNull();
  }

  @Test
  void returnPreviousUrlWhenCursorWasProvided() {
    var lookAheadResults = pricesDescending(0, 3);
    var rawCursor = encodeCursor("2024-07-01");
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(lookAheadResults);

    var response = controller.getPriceHistory(PRODUCT_ID, EUR, DESC, rawCursor, PAST, 20);

    var expectedCursor =
        encodeCursor(lookAheadResults.getFirst().getValidityPeriod().initDate().toString());

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(response.getBody().previous())
        .isEqualTo(
            "/products/"
                + PRODUCT_ID
                + "/prices?currency=EUR&cursor="
                + expectedCursor
                + "&cursorDirection=FUTURE&pageSize=20&sortOrder=DESC");
    assertThat(response.getBody().next()).isNull();
  }

  @Test
  void returnEmptyResponseWhenNoResultsFound() {
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(List.of());

    var response = controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, null, 20);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices()).isEmpty();
    assertThat(response.getBody().next()).isNull();
    assertThat(response.getBody().previous()).isNull();
  }

  @Test
  void returnReversedPricesWhenFetchDirectionOpposesDisplayOrder() {
    var ascResults = pricesAscending(0, 5);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(ascResults);

    var response =
        controller.getPriceHistory(PRODUCT_ID, EUR, DESC, encodeCursor("2024-01-01"), FUTURE, 20);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5, 19})
  void returnPriceHistoryWithClampedPageSizeWhenBelowMinimum(int belowMinPageSize) {
    when(priceHistoryReader.find(
            any(),
            any(),
            argThat(
                strategy -> {
                  var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
                  strategy.bindParameters(params);
                  return Integer.valueOf(21).equals(params.getValue("pageSize"));
                })))
        .thenReturn(List.of());

    var response = controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, null, belowMinPageSize);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
  }

  @Test
  void preservePriceContinuityWhenNavigatingNextThenPreviousInDescOrder() {
    var allPrices = pricesAscending(0, 45);

    var page1Results = simulatePastQuery(allPrices, null, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(page1Results);

    var page1Response = controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, null, 20).getBody();
    assertThat(page1Response).isNotNull();
    assertThat(page1Response.next()).isNotNull();
    assertThat(page1Response.previous()).isNull();
    assertThat(page1Response.prices()).hasSize(20);
    assertThat(page1Response.prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(page1Response.prices().getFirst().initDate()).isEqualTo(BASE_DATE.plusDays(44));
    assertThat(page1Response.prices().getLast().initDate()).isEqualTo(BASE_DATE.plusDays(25));

    var nextCursor = extractCursorDate(page1Response.next());
    var page2Results = simulatePastQuery(allPrices, nextCursor, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(page2Results);

    var page2Response =
        controller
            .getPriceHistory(PRODUCT_ID, EUR, DESC, encodeCursor(nextCursor.toString()), PAST, 20)
            .getBody();
    assertThat(page2Response).isNotNull();
    assertThat(page2Response.next()).isNotNull();
    assertThat(page2Response.previous()).isNotNull();
    assertThat(page2Response.prices()).hasSize(20);
    assertThat(page2Response.prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(page2Response.prices().getFirst().initDate()).isEqualTo(BASE_DATE.plusDays(24));
    assertThat(page2Response.prices().getLast().initDate()).isEqualTo(BASE_DATE.plusDays(5));

    var prevCursor = extractCursorDate(page2Response.previous());
    var prevResults = simulateFutureQuery(allPrices, prevCursor, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(prevResults);

    var recoveredPage1Response =
        controller
            .getPriceHistory(PRODUCT_ID, EUR, DESC, encodeCursor(prevCursor.toString()), FUTURE, 20)
            .getBody();
    assertThat(recoveredPage1Response).isNotNull();
    assertThat(recoveredPage1Response.prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.reverseOrder())
        .containsExactlyElementsOf(
            page1Response.prices().stream().map(PriceHistoryItemResponse::initDate).toList());
    assertThat(recoveredPage1Response.previous()).isNull();
    assertThat(recoveredPage1Response.next()).isNotNull();
  }

  @Test
  void preservePriceContinuityWhenNavigatingNextThenPreviousInAscOrder() {
    var allPrices = pricesAscending(0, 45);

    var page1Results = simulateFutureQuery(allPrices, null, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(page1Results);

    var page1Response = controller.getPriceHistory(PRODUCT_ID, EUR, ASC, null, null, 20).getBody();
    assertThat(page1Response).isNotNull();
    assertThat(page1Response.next()).isNotNull();
    assertThat(page1Response.previous()).isNull();
    assertThat(page1Response.prices()).hasSize(20);
    assertThat(page1Response.prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.naturalOrder());
    assertThat(page1Response.prices().getFirst().initDate()).isEqualTo(BASE_DATE.plusDays(0));
    assertThat(page1Response.prices().getLast().initDate()).isEqualTo(BASE_DATE.plusDays(19));

    var nextCursor = extractCursorDate(page1Response.next());
    var page2Results = simulateFutureQuery(allPrices, nextCursor, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(page2Results);

    var page2Response =
        controller
            .getPriceHistory(PRODUCT_ID, EUR, ASC, encodeCursor(nextCursor.toString()), FUTURE, 20)
            .getBody();
    assertThat(page2Response).isNotNull();
    assertThat(page2Response.next()).isNotNull();
    assertThat(page2Response.previous()).isNotNull();
    assertThat(page2Response.prices()).hasSize(20);
    assertThat(page2Response.prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.naturalOrder());
    assertThat(page2Response.prices().getFirst().initDate()).isEqualTo(BASE_DATE.plusDays(20));
    assertThat(page2Response.prices().getLast().initDate()).isEqualTo(BASE_DATE.plusDays(39));

    var prevCursor = extractCursorDate(page2Response.previous());
    var prevResults = simulatePastQuery(allPrices, prevCursor, 21);
    when(priceHistoryReader.find(any(), any(), any())).thenReturn(prevResults);

    var recoveredPage1Response =
        controller
            .getPriceHistory(PRODUCT_ID, EUR, ASC, encodeCursor(prevCursor.toString()), PAST, 20)
            .getBody();
    assertThat(recoveredPage1Response).isNotNull();
    assertThat(recoveredPage1Response.prices())
        .extracting(PriceHistoryItemResponse::initDate)
        .isSortedAccordingTo(Comparator.naturalOrder())
        .containsExactlyElementsOf(
            page1Response.prices().stream().map(PriceHistoryItemResponse::initDate).toList());
    assertThat(recoveredPage1Response.previous()).isNull();
    assertThat(recoveredPage1Response.next()).isNotNull();
  }

  @Test
  void failWhenProductIdIsNotUuidV7() {
    assertThatThrownBy(() -> controller.getPriceHistory(INVALID_UUID, EUR, DESC, null, null, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceHistoryReader);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenProductIdIsBlankOrNull(String invalidId) {
    assertThatThrownBy(() -> controller.getPriceHistory(invalidId, EUR, DESC, null, null, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceHistoryReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "US", "EURO", "123"})
  void failWhenCurrencyIsInvalid(String invalidCurrency) {
    assertThatThrownBy(
            () -> controller.getPriceHistory(PRODUCT_ID, invalidCurrency, DESC, null, null, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURRENCY_ISO);

    verifyNoInteractions(priceHistoryReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "UP", "DOWN", "123"})
  void failWhenSortOrderIsInvalid(String invalidSortOrder) {
    assertThatThrownBy(
            () -> controller.getPriceHistory(PRODUCT_ID, EUR, invalidSortOrder, null, null, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_SORT_ORDER);

    verifyNoInteractions(priceHistoryReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "FORWARD", "BACKWARD", "123"})
  void failWhenCursorDirectionIsInvalid(String invalidCursorDirection) {
    assertThatThrownBy(
            () ->
                controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, invalidCursorDirection, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURSOR_DIRECTION);

    verifyNoInteractions(priceHistoryReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-base64!@#$", "aW52YWxpZC1kYXRl", "MjAyNC0xMy00NQ"})
  void failWhenCursorIsInvalid(String invalidCursor) {
    assertThatThrownBy(
            () -> controller.getPriceHistory(PRODUCT_ID, EUR, DESC, invalidCursor, null, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURSOR_TOKEN);

    verifyNoInteractions(priceHistoryReader);
  }

  @ParameterizedTest
  @ValueSource(ints = {101, 200})
  void failWhenPageSizeExceedsMaximum(int oversizedPageSize) {
    assertThatThrownBy(
            () -> controller.getPriceHistory(PRODUCT_ID, EUR, DESC, null, null, oversizedPageSize))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_PAGE_SIZE);

    verifyNoInteractions(priceHistoryReader);
  }

  private List<Price> simulateFutureQuery(
      List<Price> allPrices, LocalDate cursor, int lookAheadLimit) {
    return allPrices.stream()
        .filter(p -> cursor == null || p.getValidityPeriod().initDate().isAfter(cursor))
        .sorted(
            (a, b) -> a.getValidityPeriod().initDate().compareTo(b.getValidityPeriod().initDate()))
        .limit(lookAheadLimit)
        .toList();
  }

  private List<Price> simulatePastQuery(
      List<Price> allPrices, LocalDate cursor, int lookAheadLimit) {
    return allPrices.stream()
        .filter(p -> cursor == null || p.getValidityPeriod().initDate().isBefore(cursor))
        .sorted(
            (a, b) -> b.getValidityPeriod().initDate().compareTo(a.getValidityPeriod().initDate()))
        .limit(lookAheadLimit)
        .toList();
  }

  private LocalDate extractCursorDate(String url) {
    var encoded = extractParam(url, "cursor");
    var decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    return LocalDate.parse(decoded);
  }

  private String extractParam(String url, String paramName) {
    for (String part : url.split("[?&]")) {
      if (part.startsWith(paramName + "=")) {
        return part.substring(paramName.length() + 1);
      }
    }
    throw new IllegalStateException("Param not found: " + paramName + " in " + url);
  }

  private static Price aPrice(
      String priceId, String currency, LocalDate initDate, LocalDate endDate) {
    return Price.create(
        Id.fromString(priceId),
        Id.fromString(PRODUCT_ID),
        new Money(AMOUNT, Currency.from(currency)),
        new ValidityPeriod(initDate, endDate));
  }

  private static String encodeCursor(String date) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(date.getBytes(StandardCharsets.UTF_8));
  }

  private List<Price> pricesAscending(int startOffset, int count) {
    return new ArrayList<>(
        IntStream.range(0, count)
            .mapToObj(
                i -> {
                  var date = BASE_DATE.plusDays(startOffset + i);
                  return Price.create(
                      Id.fromString(String.format("01952e42-7a57-7000-8000-%012d", i + 1)),
                      Id.fromString(PRODUCT_ID),
                      new Money(new BigDecimal("99.99"), Currency.DEFAULT),
                      new ValidityPeriod(date, date.plusDays(1)));
                })
            .toList());
  }

  private List<Price> pricesDescending(int startOffset, int count) {
    return new ArrayList<>(
        IntStream.range(0, count)
            .mapToObj(
                i -> {
                  var date = BASE_DATE.plusDays(startOffset + count - 1 - i);
                  return Price.create(
                      Id.fromString(String.format("01952e42-7a57-7000-8000-%012d", i + 1)),
                      Id.fromString(PRODUCT_ID),
                      new Money(new BigDecimal("99.99"), Currency.DEFAULT),
                      new ValidityPeriod(date, date.plusDays(1)));
                })
            .toList());
  }
}
