package com.mango.products.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mango.products.IntegrationTestBase;
import com.mango.products.domain.Currency;
import com.mango.products.domain.Description;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Name;
import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import com.mango.products.domain.ValidityPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public abstract class UpdatePriceFeature extends IntegrationTestBase {

  @Test
  void shouldUpdatePriceSuccessfully() throws Exception {
    var productId = generateUUIDv7();
    var priceId = generateUUIDv7();

    var product =
        Product.create(
            new Id(productId),
            new Name("Zapatillas deportivas"),
            new Description("Modelo 2025 edición limitada"));
    productRepository.save(product);

    var price =
        Price.create(
            new Id(priceId),
            new Id(productId),
            new Money(new BigDecimal("99.99"), Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)));
    priceRepository.save(price);

    String updateRequestBody =
        """
        {
          "value": 149.99,
          "currency": "EUR",
          "initDate": "2024-01-01",
          "endDate": "2024-08-31"
        }
        """;

    String expectedResponseBody =
        """
        {
          "id": "%s",
          "value": 149.99,
          "currency": "EUR",
          "initDate": "2024-01-01",
          "endDate": "2024-08-31"
        }
        """
            .formatted(priceId);

    mockMvc
        .perform(
            put("/products/" + productId + "/prices/" + priceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequestBody))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedResponseBody));
  }
}
