package com.mango.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mango.products.domain.Id;
import com.mango.products.domain.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletePriceUseCaseShould {

  private static final String PRICE_ID = "01952e42-7a57-7000-8000-000000000002";
  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_ID_BLANK = "Id must not be blank";

  @Mock private PriceRepository priceRepository;

  private DeletePriceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new DeletePriceUseCase(priceRepository);
  }

  @Test
  void deletePriceSuccessfullyWhenExists() {
    var command = new DeletePriceCommand(PRICE_ID, PRODUCT_ID);
    when(priceRepository.deleteById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID)))
        .thenReturn(true);

    boolean deleted = useCase.execute(command);

    assertThat(deleted).isTrue();
    verify(priceRepository).deleteById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID));
  }

  @Test
  void returnFalseWhenPriceDoesNotExist() {
    var command = new DeletePriceCommand(PRICE_ID, PRODUCT_ID);
    when(priceRepository.deleteById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID)))
        .thenReturn(false);

    boolean deleted = useCase.execute(command);

    assertThat(deleted).isFalse();
    verify(priceRepository).deleteById(Id.fromString(PRICE_ID), Id.fromString(PRODUCT_ID));
  }

  @Test
  void failWhenPriceIdIsNotUuidV7() {
    var command = new DeletePriceCommand(INVALID_UUID, PRODUCT_ID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenPriceIdIsBlankOrNull(String invalidId) {
    var command = new DeletePriceCommand(invalidId, PRODUCT_ID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }

  @Test
  void failWhenProductIdIsNotUuidV7() {
    var command = new DeletePriceCommand(PRICE_ID, INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(priceRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenProductIdIsBlankOrNull(String invalidId) {
    var command = new DeletePriceCommand(PRICE_ID, invalidId);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_ID_BLANK);

    verifyNoInteractions(priceRepository);
  }
}
