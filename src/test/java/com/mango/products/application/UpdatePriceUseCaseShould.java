package com.mango.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mango.products.domain.Currency;
import com.mango.products.domain.DomainException.AmountMustBePositiveException;
import com.mango.products.domain.DomainException.BlankIdException;
import com.mango.products.domain.DomainException.InactivePriceUpdateException;
import com.mango.products.domain.DomainException.InitDateNotBeforeEndDateException;
import com.mango.products.domain.DomainException.InvalidCurrencyCodeException;
import com.mango.products.domain.DomainException.InvalidUuidV7Exception;
import com.mango.products.domain.DomainException.NullInitDateException;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
class UpdatePriceUseCaseShould {

  private static final String PRICE_ID = "01952e42-7a57-7000-8000-000000000002";
  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final BigDecimal AMOUNT = new BigDecimal("149.99");
  private static final String EUR = "EUR";
  private static final String USD = "USD";
  private static final Instant FIXED_NOW = Instant.parse("2024-04-15T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
  private static final LocalDate ACTIVE_INIT_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate ACTIVE_END_DATE = LocalDate.of(2024, 6, 30);
  private static final LocalDate NEW_INIT_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate NEW_END_DATE = LocalDate.of(2024, 8, 31);

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_ID_BLANK = "Id must not be blank";
  private static final String ERROR_AMOUNT_POSITIVE = "Amount must be greater than zero";
  private static final String ERROR_CURRENCY_ISO = "Currency must be a valid ISO-4217 code";
  private static final String ERROR_INIT_DATE_NULL = "Init date must not be null";
  private static final String ERROR_INIT_DATE_BEFORE_END = "Init date must be before end date";
  private static final String ERROR_NOT_ACTIVE = "Only currently active prices can be updated";

  @Mock private PriceRepository priceRepository;
  @Captor private ArgumentCaptor<Price> priceCaptor;

  private UpdatePriceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new UpdatePriceUseCase(priceRepository, FIXED_CLOCK);
  }

  @Test
  void updateAndPersistPriceWithExplicitCurrencyWhenCurrentlyActive() {
    var existingActivePrice = anExistingPrice(EUR, ACTIVE_INIT_DATE, ACTIVE_END_DATE);
    when(priceRepository.findById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID)))
        .thenReturn(Optional.of(existingActivePrice));

    var command = aCommandWithCurrency(USD);
    Optional<Price> updated = useCase.execute(command);

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.from(USD)),
            new ValidityPeriod(NEW_INIT_DATE, NEW_END_DATE));

    assertThat(updated).isPresent();
    assertThat(updated.get()).usingRecursiveComparison().isEqualTo(expectedPrice);
    verify(priceRepository).update(priceCaptor.capture());
    Price savedPrice = priceCaptor.getValue();
    assertThat(savedPrice).usingRecursiveComparison().isEqualTo(expectedPrice);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void updateAndPersistPriceWithDefaultCurrencyWhenOmittedOrBlank(String rawCurrency) {
    var existingActivePrice = anExistingPrice(EUR, ACTIVE_INIT_DATE, ACTIVE_END_DATE);
    when(priceRepository.findById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID)))
        .thenReturn(Optional.of(existingActivePrice));

    var command = aCommandWithCurrency(rawCurrency);
    Optional<Price> updated = useCase.execute(command);

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(NEW_INIT_DATE, NEW_END_DATE));

    assertThat(updated).isPresent();
    assertThat(updated.get()).usingRecursiveComparison().isEqualTo(expectedPrice);
    verify(priceRepository).update(priceCaptor.capture());
    Price savedPrice = priceCaptor.getValue();
    assertThat(savedPrice).usingRecursiveComparison().isEqualTo(expectedPrice);
  }

  @Test
  void returnEmptyOptionalWhenPriceToUpdateDoesNotExist() {
    when(priceRepository.findById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID)))
        .thenReturn(Optional.empty());

    var command = aCommandWithCurrency(EUR);
    Optional<Price> updated = useCase.execute(command);

    assertThat(updated).isEmpty();
  }

  @Test
  void failWhenPriceIsNotCurrentlyActive() {
    var pastPrice = anExistingPrice(EUR, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
    when(priceRepository.findById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID)))
        .thenReturn(Optional.of(pastPrice));

    var command = aCommandWithCurrency(EUR);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InactivePriceUpdateException.class)
        .hasMessage(ERROR_NOT_ACTIVE);
  }

  @Test
  void failWhenPriceIdIsNotUuidV7() {
    var command = aCommandWithPriceId(INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidUuidV7Exception.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenPriceIdIsBlankOrNull(String invalidId) {
    var command = aCommandWithPriceId(invalidId);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(BlankIdException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenProductIdIsNotUuidV7() {
    var command = aCommandWithProductId(INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidUuidV7Exception.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenProductIdIsBlankOrNull(String invalidId) {
    var command = aCommandWithProductId(invalidId);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(BlankIdException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenAmountIsNull() {
    var command = aCommandWithAmount(null);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(AmountMustBePositiveException.class)
        .hasMessage(ERROR_AMOUNT_POSITIVE);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.00", "-1.00", "-99.99"})
  void failWhenAmountIsZeroOrNegative(String invalidAmount) {
    var command = aCommandWithAmount(new BigDecimal(invalidAmount));

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(AmountMustBePositiveException.class)
        .hasMessage(ERROR_AMOUNT_POSITIVE);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "US", "EURO", "123"})
  void failWhenCurrencyIsInvalid(String invalidCurrency) {
    var command = aCommandWithCurrency(invalidCurrency);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InvalidCurrencyCodeException.class)
        .hasMessage(ERROR_CURRENCY_ISO);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenInitDateIsNull() {
    var command = aCommandWithDates(null, NEW_END_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(NullInitDateException.class)
        .hasMessage(ERROR_INIT_DATE_NULL);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenInitDateIsAfterEndDate() {
    var command = aCommandWithDates(NEW_END_DATE, NEW_INIT_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InitDateNotBeforeEndDateException.class)
        .hasMessage(ERROR_INIT_DATE_BEFORE_END);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenInitDateIsEqualEndDate() {
    var command = aCommandWithDates(NEW_END_DATE, NEW_END_DATE);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(InitDateNotBeforeEndDateException.class)
        .hasMessage(ERROR_INIT_DATE_BEFORE_END);

    verifyNoInteractions(priceRepository);
  }

  private static Price anExistingPrice(String currency, LocalDate initDate, LocalDate endDate) {
    return Price.create(
        Id.fromString(PRICE_ID),
        Id.fromString(PRODUCT_ID),
        new Money(new BigDecimal("99.99"), Currency.from(currency)),
        new ValidityPeriod(initDate, endDate));
  }

  private static UpdatePriceCommand aCommandWithPriceId(String priceId) {
    return new UpdatePriceCommand(priceId, PRODUCT_ID, AMOUNT, EUR, NEW_INIT_DATE, NEW_END_DATE);
  }

  private static UpdatePriceCommand aCommandWithProductId(String productId) {
    return new UpdatePriceCommand(PRICE_ID, productId, AMOUNT, EUR, NEW_INIT_DATE, NEW_END_DATE);
  }

  private static UpdatePriceCommand aCommandWithAmount(BigDecimal amount) {
    return new UpdatePriceCommand(PRICE_ID, PRODUCT_ID, amount, EUR, NEW_INIT_DATE, NEW_END_DATE);
  }

  private static UpdatePriceCommand aCommandWithCurrency(String currency) {
    return new UpdatePriceCommand(
        PRICE_ID, PRODUCT_ID, AMOUNT, currency, NEW_INIT_DATE, NEW_END_DATE);
  }

  private static UpdatePriceCommand aCommandWithDates(LocalDate initDate, LocalDate endDate) {
    return new UpdatePriceCommand(PRICE_ID, PRODUCT_ID, AMOUNT, EUR, initDate, endDate);
  }
}
