package com.mango.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mango.products.domain.Description;
import com.mango.products.domain.Id;
import com.mango.products.domain.Name;
import com.mango.products.domain.Product;
import com.mango.products.domain.ProductRepository;
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
  private static final String INVALID_UUID = "c9bf9e57-1685-4c89-bafb-ff5af830be8a";
  private static final String NAME = "Zapatillas deportivas";
  private static final String DESCRIPTION = "Modelo 2025 edición limitada";

  private static final String ERROR_UUID_V7 = "Id must be a valid UUIDv7";
  private static final String ERROR_NAME_BLANK = "Name cannot be blank";
  private static final String ERROR_DESCRIPTION_BLANK = "Description cannot be blank";

  @Mock private ProductRepository productRepository;
  @Captor private ArgumentCaptor<Product> productCaptor;

  private CreateProductUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateProductUseCase(productRepository);
  }

  @Test
  void createAndPersistProductWithExplicitId() {
    var command = new CreateProductCommand(PRODUCT_ID, NAME, DESCRIPTION);

    Product result = useCase.execute(command);

    verify(productRepository).save(productCaptor.capture());
    Product savedProduct = productCaptor.getValue();

    var expectedProduct =
        Product.create(Id.fromString(PRODUCT_ID), new Name(NAME), new Description(DESCRIPTION));
    assertThat(savedProduct).usingRecursiveComparison().isEqualTo(expectedProduct);
    assertThat(result).usingRecursiveComparison().isEqualTo(expectedProduct);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void createAndPersistProductWithGeneratedIdWhenOmittedOrBlank(String rawId) {
    var command = aCommandWithId(rawId);

    Product result = useCase.execute(command);

    verify(productRepository).save(productCaptor.capture());
    Product savedProduct = productCaptor.getValue();

    assertThat(savedProduct.getId()).isNotNull();
    assertThat(savedProduct.getId().value().version()).isEqualTo(7);
    assertThat(savedProduct.getName().value()).isEqualTo(NAME);
    assertThat(savedProduct.getDescription().value()).isEqualTo(DESCRIPTION);
    assertThat(result).usingRecursiveComparison().isEqualTo(savedProduct);
  }

  @Test
  void failWhenIdIsNotUuidV7() {
    var command = aCommandWithId(INVALID_UUID);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_UUID_V7);

    verifyNoInteractions(productRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenNameIsBlankOrNull(String invalidName) {
    var command = aCommandWithName(invalidName);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_NAME_BLANK);

    verifyNoInteractions(productRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void failWhenDescriptionIsBlankOrNull(String invalidDescription) {
    var command = aCommandWithDescription(invalidDescription);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(ERROR_DESCRIPTION_BLANK);

    verifyNoInteractions(productRepository);
  }

  private static CreateProductCommand aCommandWithId(String id) {
    return new CreateProductCommand(id, NAME, DESCRIPTION);
  }

  private static CreateProductCommand aCommandWithName(String name) {
    return new CreateProductCommand(PRODUCT_ID, name, DESCRIPTION);
  }

  private static CreateProductCommand aCommandWithDescription(String description) {
    return new CreateProductCommand(PRODUCT_ID, NAME, description);
  }
}
