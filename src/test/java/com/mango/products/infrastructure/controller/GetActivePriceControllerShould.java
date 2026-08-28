package com.mango.products.infrastructure.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.ValidityPeriod;
import com.mango.products.infrastructure.controller.readmode.GetActivePriceController;
import com.mango.products.infrastructure.controller.readmode.GetActivePriceResponse;
import com.mango.products.infrastructure.repository.ActivePriceReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetActivePriceControllerShould {

  private static final String PRICE_ID = "01952e42-7a57-7000-8000-000000000002";
  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final BigDecimal AMOUNT = new BigDecimal("99.99");
  private static final String EUR = "EUR";
  private static final String USD = "USD";
  private static final String QUERY_DATE_STR = "2024-04-15";
  private static final LocalDate QUERY_DATE = LocalDate.of(2024, 4, 15);
  private static final LocalDate INIT_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2024, 6, 30);

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_ID_BLANK = "Id must not be blank";
  private static final String ERROR_DATE_BLANK = "Date must not be blank";
  private static final String ERROR_DATE_FORMAT = "Date must be in ISO-8601 format (YYYY-MM-DD)";
  private static final String ERROR_CURRENCY_ISO = "Currency must be a valid ISO-4217 code";

  @Mock private ActivePriceReader activePriceReader;

  private GetActivePriceController controller;

  @BeforeEach
  void setUp() {
    controller = new GetActivePriceController(activePriceReader);
  }

  @Test
  void returnActivePriceForProductAndDateWithExplicitCurrency() {
    var price = aPrice(USD);
    when(activePriceReader.findActivePrice(
            Id.fromString(PRODUCT_ID), QUERY_DATE, Currency.from(USD)))
        .thenReturn(Optional.of(price));

    var response = controller.getActivePrice(PRODUCT_ID, QUERY_DATE_STR, USD);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo(new GetActivePriceResponse(AMOUNT, USD));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void returnActivePriceForProductAndDateWithDefaultCurrencyWhenOmittedOrBlank(String rawCurrency) {
    var price = aPrice(EUR);
    when(activePriceReader.findActivePrice(Id.fromString(PRODUCT_ID), QUERY_DATE, Currency.DEFAULT))
        .thenReturn(Optional.of(price));

    var response = controller.getActivePrice(PRODUCT_ID, QUERY_DATE_STR, rawCurrency);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo(new GetActivePriceResponse(AMOUNT, EUR));
  }

  @Test
  void returnNotFoundWhenNoActivePriceFound() {
    when(activePriceReader.findActivePrice(Id.fromString(PRODUCT_ID), QUERY_DATE, Currency.DEFAULT))
        .thenReturn(Optional.empty());

    var response = controller.getActivePrice(PRODUCT_ID, QUERY_DATE_STR, null);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void failWhenProductIdIsNotUuidV7() {
    assertThatThrownBy(() -> controller.getActivePrice(INVALID_UUID, QUERY_DATE_STR, EUR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(activePriceReader);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenProductIdIsBlankOrNull(String invalidId) {
    assertThatThrownBy(() -> controller.getActivePrice(invalidId, QUERY_DATE_STR, EUR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(activePriceReader);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenDateIsBlankOrNull(String invalidDate) {
    assertThatThrownBy(() -> controller.getActivePrice(PRODUCT_ID, invalidDate, EUR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_DATE_BLANK);

    verifyNoInteractions(activePriceReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid-date", "2024/04/15", "15-04-2024", "2024-13-01"})
  void failWhenDateFormatIsInvalid(String invalidDate) {
    assertThatThrownBy(() -> controller.getActivePrice(PRODUCT_ID, invalidDate, EUR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_DATE_FORMAT);

    verifyNoInteractions(activePriceReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "US", "EURO", "123"})
  void failWhenCurrencyIsInvalid(String invalidCurrency) {
    assertThatThrownBy(() -> controller.getActivePrice(PRODUCT_ID, QUERY_DATE_STR, invalidCurrency))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURRENCY_ISO);

    verifyNoInteractions(activePriceReader);
  }

  private static Price aPrice(String currency) {
    return Price.create(
        Id.fromString(PRICE_ID),
        Id.fromString(PRODUCT_ID),
        new Money(AMOUNT, Currency.from(currency)),
        new ValidityPeriod(INIT_DATE, END_DATE));
  }
}
