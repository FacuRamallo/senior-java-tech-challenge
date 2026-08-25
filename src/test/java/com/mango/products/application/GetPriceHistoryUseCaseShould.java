package com.mango.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPriceHistoryUseCaseShould {

  private static final String PRICE_ID_1 = "01952e42-7a57-7000-8000-000000000002";
  private static final String PRICE_ID_2 = "01952e42-7a57-7000-8000-000000000003";
  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final BigDecimal AMOUNT_1 = new BigDecimal("99.99");
  private static final BigDecimal AMOUNT_2 = new BigDecimal("149.99");
  private static final String EUR = "EUR";
  private static final String USD = "USD";
  private static final LocalDate INIT_DATE_1 = LocalDate.of(2024, 1, 1);
  private static final LocalDate END_DATE_1 = LocalDate.of(2024, 6, 30);
  private static final LocalDate INIT_DATE_2 = LocalDate.of(2024, 7, 1);
  private static final LocalDate END_DATE_2 = LocalDate.of(2024, 12, 31);

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_ID_BLANK = "Id must not be blank";
  private static final String ERROR_CURRENCY_ISO = "Currency must be a valid ISO-4217 code";

  @Mock private PriceRepository priceRepository;

  private GetPriceHistoryUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new GetPriceHistoryUseCase(priceRepository);
  }

  @Test
  void returnPriceHistoryForProductWithExplicitCurrency() {
    var query = aQueryWithCurrency(USD);

    var price1 =
        Price.create(
            Id.fromString(PRICE_ID_1),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT_1, Currency.from(USD)),
            new ValidityPeriod(INIT_DATE_1, END_DATE_1));
    var price2 =
        Price.create(
            Id.fromString(PRICE_ID_2),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT_2, Currency.from(USD)),
            new ValidityPeriod(INIT_DATE_2, END_DATE_2));

    var expectedPrices = List.of(price1, price2);

    when(priceRepository.findPriceHistory(Id.fromString(PRODUCT_ID), Currency.from(USD)))
        .thenReturn(expectedPrices);

    List<Price> actualPrices = useCase.execute(query);

    assertThat(actualPrices).containsExactlyElementsOf(expectedPrices);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void returnPriceHistoryForProductWithDefaultCurrencyWhenOmittedOrBlank(String rawCurrency) {
    var query = aQueryWithCurrency(rawCurrency);

    var price1 =
        Price.create(
            Id.fromString(PRICE_ID_1),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT_1, Currency.DEFAULT),
            new ValidityPeriod(INIT_DATE_1, END_DATE_1));
    var price2 =
        Price.create(
            Id.fromString(PRICE_ID_2),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT_2, Currency.DEFAULT),
            new ValidityPeriod(INIT_DATE_2, END_DATE_2));

    var expectedPrices = List.of(price1, price2);

    when(priceRepository.findPriceHistory(Id.fromString(PRODUCT_ID), Currency.DEFAULT))
        .thenReturn(expectedPrices);

    List<Price> actualPrices = useCase.execute(query);

    assertThat(actualPrices).containsExactlyElementsOf(expectedPrices);
  }

  @Test
  void returnEmptyListWhenNoPricesFound() {
    var query = aValidQuery();

    when(priceRepository.findPriceHistory(Id.fromString(PRODUCT_ID), Currency.DEFAULT))
        .thenReturn(List.of());

    List<Price> actualPrices = useCase.execute(query);

    assertThat(actualPrices).isEmpty();
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
  @ValueSource(strings = {"INVALID", "US", "EURO", "123"})
  void failWhenCurrencyIsInvalid(String invalidCurrency) {
    var query = aQueryWithCurrency(invalidCurrency);

    assertThatThrownBy(() -> useCase.execute(query))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURRENCY_ISO);

    verifyNoInteractions(priceRepository);
  }

  private static GetPriceHistoryQuery aValidQuery() {
    return new GetPriceHistoryQuery(PRODUCT_ID, EUR);
  }

  private static GetPriceHistoryQuery aQueryWithProductId(String productId) {
    return new GetPriceHistoryQuery(productId, EUR);
  }

  private static GetPriceHistoryQuery aQueryWithCurrency(String currency) {
    return new GetPriceHistoryQuery(PRODUCT_ID, currency);
  }
}
