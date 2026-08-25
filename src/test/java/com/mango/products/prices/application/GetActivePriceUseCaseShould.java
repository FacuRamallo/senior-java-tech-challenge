package com.mango.products.prices.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mango.products.prices.domain.Currency;
import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Money;
import com.mango.products.prices.domain.Price;
import com.mango.products.prices.domain.PriceRepository;
import com.mango.products.prices.domain.ValidityPeriod;
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
class GetActivePriceUseCaseShould {

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

  @Mock private PriceRepository priceRepository;

  private GetActivePriceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new GetActivePriceUseCase(priceRepository);
  }

  @Test
  void returnActivePriceForProductAndDateWithExplicitCurrency() {
    var query = aQueryWithCurrency(USD);

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.from(USD)),
            new ValidityPeriod(INIT_DATE, END_DATE));

    when(priceRepository.findActivePrice(Id.fromString(PRODUCT_ID), QUERY_DATE, Currency.from(USD)))
        .thenReturn(Optional.of(expectedPrice));

    Optional<Price> actualPrice = useCase.execute(query);

    assertThat(actualPrice).contains(expectedPrice);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void returnActivePriceForProductAndDateWithDefaultCurrencyWhenOmittedOrBlank(String rawCurrency) {
    var query = aQueryWithCurrency(rawCurrency);

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(INIT_DATE, END_DATE));

    when(priceRepository.findActivePrice(Id.fromString(PRODUCT_ID), QUERY_DATE, Currency.DEFAULT))
        .thenReturn(Optional.of(expectedPrice));

    Optional<Price> actualPrice = useCase.execute(query);

    assertThat(actualPrice).contains(expectedPrice);
  }

  @Test
  void returnEmptyWhenNoActivePriceFound() {
    var query = aValidQuery();

    when(priceRepository.findActivePrice(Id.fromString(PRODUCT_ID), QUERY_DATE, Currency.DEFAULT))
        .thenReturn(Optional.empty());

    Optional<Price> actualPrice = useCase.execute(query);

    assertThat(actualPrice).isEmpty();
  }

  @Test
  void failWhenProductIdIsNotUuidV7() {
    var query = aQueryWithProductId(INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(query))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenProductIdIsBlankOrNull(String invalidId) {
    var query = aQueryWithProductId(invalidId);

    assertThatThrownBy(() -> useCase.execute(query))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenDateIsBlankOrNull(String invalidDate) {
    var query = aQueryWithDate(invalidDate);

    assertThatThrownBy(() -> useCase.execute(query))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_DATE_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid-date", "2024/04/15", "15-04-2024", "2024-13-01"})
  void failWhenDateFormatIsInvalid(String invalidDate) {
    var query = aQueryWithDate(invalidDate);

    assertThatThrownBy(() -> useCase.execute(query))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_DATE_FORMAT);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "US", "EURO", "123"})
  void failWhenCurrencyIsInvalid(String invalidCurrency) {
    var query = aQueryWithCurrency(invalidCurrency);

    assertThatThrownBy(() -> useCase.execute(query))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURRENCY_ISO);

    verifyNoInteractions(priceRepository);
  }

  private static GetActivePriceQuery aValidQuery() {
    return new GetActivePriceQuery(PRODUCT_ID, QUERY_DATE_STR, EUR);
  }

  private static GetActivePriceQuery aQueryWithProductId(String productId) {
    return new GetActivePriceQuery(productId, QUERY_DATE_STR, EUR);
  }

  private static GetActivePriceQuery aQueryWithDate(String date) {
    return new GetActivePriceQuery(PRODUCT_ID, date, EUR);
  }

  private static GetActivePriceQuery aQueryWithCurrency(String currency) {
    return new GetActivePriceQuery(PRODUCT_ID, QUERY_DATE_STR, currency);
  }
}
