package com.mango.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class UpdatePriceUseCaseShould {

  private static final String PRICE_ID = "01952e42-7a57-7000-8000-000000000002";
  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final BigDecimal AMOUNT = new BigDecimal("149.99");
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

  @Mock private PriceRepository priceRepository;
  @Captor private ArgumentCaptor<Price> priceCaptor;

  private UpdatePriceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new UpdatePriceUseCase(priceRepository);
  }

  @Test
  void updateAndPersistPriceWithExplicitCurrency() {
    var command = aCommandWithCurrency(USD);
    when(priceRepository.update(any(Price.class))).thenReturn(true);

    Optional<Price> updated = useCase.execute(command);

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.from(USD)),
            new ValidityPeriod(INIT_DATE, END_DATE));

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
    var command = aCommandWithCurrency(rawCurrency);
    when(priceRepository.update(any(Price.class))).thenReturn(true);

    Optional<Price> updated = useCase.execute(command);

    var expectedPrice =
        Price.create(
            Id.fromString(PRICE_ID),
            Id.fromString(PRODUCT_ID),
            new Money(AMOUNT, Currency.DEFAULT),
            new ValidityPeriod(INIT_DATE, END_DATE));

    assertThat(updated).isPresent();
    assertThat(updated.get()).usingRecursiveComparison().isEqualTo(expectedPrice);
    verify(priceRepository).update(priceCaptor.capture());
    Price savedPrice = priceCaptor.getValue();
    assertThat(savedPrice).usingRecursiveComparison().isEqualTo(expectedPrice);
  }

  @Test
  void returnEmptyOptionalWhenPriceToUpdateDoesNotExist() {
    var command = aCommandWithCurrency(EUR);
    when(priceRepository.update(any(Price.class))).thenReturn(false);

    Optional<Price> updated = useCase.execute(command);

    assertThat(updated).isEmpty();
    verify(priceRepository).update(any(Price.class));
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

  private static UpdatePriceCommand aCommandWithPriceId(String priceId) {
    return new UpdatePriceCommand(priceId, PRODUCT_ID, AMOUNT, EUR, INIT_DATE, END_DATE);
  }

  private static UpdatePriceCommand aCommandWithProductId(String productId) {
    return new UpdatePriceCommand(PRICE_ID, productId, AMOUNT, EUR, INIT_DATE, END_DATE);
  }

  private static UpdatePriceCommand aCommandWithAmount(BigDecimal amount) {
    return new UpdatePriceCommand(PRICE_ID, PRODUCT_ID, amount, EUR, INIT_DATE, END_DATE);
  }

  private static UpdatePriceCommand aCommandWithCurrency(String currency) {
    return new UpdatePriceCommand(PRICE_ID, PRODUCT_ID, AMOUNT, currency, INIT_DATE, END_DATE);
  }

  private static UpdatePriceCommand aCommandWithDates(LocalDate initDate, LocalDate endDate) {
    return new UpdatePriceCommand(PRICE_ID, PRODUCT_ID, AMOUNT, EUR, initDate, endDate);
  }
}
