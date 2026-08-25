package com.mango.products.prices.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mango.products.prices.domain.Description;
import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Name;
import com.mango.products.prices.domain.Product;
import com.mango.products.prices.domain.ProductRepository;
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
class CreateProductUseCaseShould {

  private static final String PRODUCT_ID = "01952e42-7a57-7000-8000-000000000001";
  private static final String NAME = "Zapatillas deportivas";
  private static final String DESCRIPTION = "Modelo 2025 edición limitada";

  @Mock private ProductRepository productRepository;
  @Captor private ArgumentCaptor<Product> productCaptor;

  private CreateProductUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateProductUseCase(productRepository);
  }

  @Test
  void createAndPersistProduct() {
    var command = new CreateProductCommand(PRODUCT_ID, NAME, DESCRIPTION);

    useCase.execute(command);

    verify(productRepository).save(productCaptor.capture());
    Product savedProduct = productCaptor.getValue();

    var expectedProduct =
        Product.create(Id.fromString(PRODUCT_ID), new Name(NAME), new Description(DESCRIPTION));
    assertThat(savedProduct).usingRecursiveComparison().isEqualTo(expectedProduct);
  }

  @Test
  void failWhenIdIsNotUuidV7() {
    var invalidId = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
    var command = new CreateProductCommand(invalidId, NAME, DESCRIPTION);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Id must be a valid UUIDv7");

    verifyNoInteractions(productRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenIdIsBlankOrNull(String invalidId) {
    var command = new CreateProductCommand(invalidId, NAME, DESCRIPTION);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Id must not be blank");

    verifyNoInteractions(productRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenNameIsBlankOrNull(String invalidName) {
    var command = new CreateProductCommand(PRODUCT_ID, invalidName, DESCRIPTION);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Name cannot be blank");

    verifyNoInteractions(productRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenDescriptionIsBlankOrNull(String invalidDescription) {
    var command = new CreateProductCommand(PRODUCT_ID, NAME, invalidDescription);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Description cannot be blank");

    verifyNoInteractions(productRepository);
  }
}
