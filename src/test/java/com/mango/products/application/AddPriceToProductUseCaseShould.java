package com.mango.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddPriceToProductUseCaseShould {

  private static final String PRICE_ID = "01952e42-7a57-7000-8000-000000000002";
  private static final String PREVIOUS_PRICE_ID = "01952e42-7a57-7000-8000-000000000003";
  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final BigDecimal AMOUNT = new BigDecimal("99.99");
  private static final String EUR = "EUR";
  private static final String USD = "USD";
  private static final LocalDate INIT_DATE = LocalDate.of(2024, 7, 1);
  private static final LocalDate END_DATE = LocalDate.of(2024, 12, 31);

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_ID_BLANK = "Id must not be blank";
  private static final String ERROR_AMOUNT_POSITIVE = "Amount must be greater than zero";
  private static final String ERROR_CURRENCY_ISO = "Currency must be a valid ISO-4217 code";
  private static final String ERROR_INIT_DATE_NULL = "Init date must not be null";
  private static final String ERROR_INIT_DATE_BEFORE_END = "Init date must be before end date";
  private static final String ERROR_OPEN_ENDED_PRICE =
      "Cannot create a new price while the current price has an open-ended end date";
  private static final String ERROR_INIT_NOT_AFTER_LAST_END =
      "New price init date must be after the last price end date";

  @Mock private PriceRepository priceRepository;
  @Captor private ArgumentCaptor<Price> priceCaptor;

  private AddPriceToProductUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new AddPriceToProductUseCase(priceRepository);
  }

  @Test
  void addAndPersistPriceWithExplicitCurrencyWhenNoPreviousPrice() {
    var command = aCommandWithCurrency(USD);
    when(priceRepository.findLatestPrice(eq(Id.fromString(PRODUCT_ID)), eq(Currency.from(USD))))
        .thenReturn(Optional.empty());

    useCase.execute(command);

    verify(priceRepository).save(priceCaptor.capture());
    Price savedPrice = priceCaptor.getValue();

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.from(USD)),
            new ValidityPeriod(INIT_DATE, END_DATE));

    assertThat(savedPrice).usingRecursiveComparison().isEqualTo(expectedPrice);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void addAndPersistPriceWithDefaultCurrencyWhenOmittedOrBlank(String rawCurrency) {
    var command = aCommandWithCurrency(rawCurrency);
    when(priceRepository.findLatestPrice(eq(Id.fromString(PRODUCT_ID)), eq(Currency.DEFAULT)))
        .thenReturn(Optional.empty());

    useCase.execute(command);

    verify(priceRepository).save(priceCaptor.capture());
    Price savedPrice = priceCaptor.getValue();

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(INIT_DATE, END_DATE));

    assertThat(savedPrice).usingRecursiveComparison().isEqualTo(expectedPrice);
  }

  @Test
  void addAndPersistPriceWhenPreviousPriceIsClosedBeforeInitDate() {
    var previousPrice =
        Price.create(
            Id.fromString(PREVIOUS_PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)));
    when(priceRepository.findLatestPrice(eq(Id.fromString(PRODUCT_ID)), eq(Currency.DEFAULT)))
        .thenReturn(Optional.of(previousPrice));

    var command = aCommandWithDates(INIT_DATE, END_DATE);

    useCase.execute(command);

    verify(priceRepository).save(priceCaptor.capture());
    Price savedPrice = priceCaptor.getValue();

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(INIT_DATE, END_DATE));

    assertThat(savedPrice).usingRecursiveComparison().isEqualTo(expectedPrice);
  }

  @Test
  void failWhenLatestPriceHasOpenEndedEndDate() {
    var openEndedPreviousPrice =
        Price.create(
            Id.fromString(PREVIOUS_PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 1, 1), null));
    when(priceRepository.findLatestPrice(eq(Id.fromString(PRODUCT_ID)), eq(Currency.DEFAULT)))
        .thenReturn(Optional.of(openEndedPreviousPrice));

    var command = aCommandWithDates(INIT_DATE, END_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_OPEN_ENDED_PRICE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"2024-06-30", "2024-06-15", "2024-01-01"})
  void failWhenInitDateIsNotAfterLatestPriceEndDate(String conflictingInitDate) {
    var previousPrice =
        Price.create(
            Id.fromString(PREVIOUS_PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)));
    when(priceRepository.findLatestPrice(eq(Id.fromString(PRODUCT_ID)), eq(Currency.DEFAULT)))
        .thenReturn(Optional.of(previousPrice));

    var command = aCommandWithDates(LocalDate.parse(conflictingInitDate), END_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_INIT_NOT_AFTER_LAST_END);
  }

  @Test
  void failWhenPriceIdIsNotUuidV7() {
    var command = aCommandWithPriceId(INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenPriceIdIsBlankOrNull(String invalidId) {
    var command = aCommandWithPriceId(invalidId);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenProductIdIsNotUuidV7() {
    var command = aCommandWithProductId(INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenProductIdIsBlankOrNull(String invalidId) {
    var command = aCommandWithProductId(invalidId);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenAmountIsNull() {
    var command = aCommandWithAmount(null);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_AMOUNT_POSITIVE);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.00", "-1.00", "-99.99"})
  void failWhenAmountIsZeroOrNegative(String invalidAmount) {
    var command = aCommandWithAmount(new BigDecimal(invalidAmount));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_AMOUNT_POSITIVE);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "US", "EURO", "123"})
  void failWhenCurrencyIsInvalid(String invalidCurrency) {
    var command = aCommandWithCurrency(invalidCurrency);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_CURRENCY_ISO);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenInitDateIsNull() {
    var command = aCommandWithDates(null, END_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_INIT_DATE_NULL);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenInitDateIsAfterEndDate() {
    var command = aCommandWithDates(END_DATE, INIT_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_INIT_DATE_BEFORE_END);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenInitDateIsEqualEndDate() {
    var command = aCommandWithDates(END_DATE, END_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_INIT_DATE_BEFORE_END);

    verifyNoInteractions(priceRepository);
  }

  private static AddPriceToProductCommand aCommandWithPriceId(String priceId) {
    return new AddPriceToProductCommand(priceId, PRODUCT_ID, AMOUNT, EUR, INIT_DATE, END_DATE);
  }

  private static AddPriceToProductCommand aCommandWithProductId(String productId) {
    return new AddPriceToProductCommand(PRICE_ID, productId, AMOUNT, EUR, INIT_DATE, END_DATE);
  }

  private static AddPriceToProductCommand aCommandWithAmount(BigDecimal amount) {
    return new AddPriceToProductCommand(PRICE_ID, PRODUCT_ID, amount, EUR, INIT_DATE, END_DATE);
  }

  private static AddPriceToProductCommand aCommandWithCurrency(String currency) {
    return new AddPriceToProductCommand(
        PRICE_ID, PRODUCT_ID, AMOUNT, currency, INIT_DATE, END_DATE);
  }

  private static AddPriceToProductCommand aCommandWithDates(LocalDate initDate, LocalDate endDate) {
    return new AddPriceToProductCommand(PRICE_ID, PRODUCT_ID, AMOUNT, EUR, initDate, endDate);
  }
}
