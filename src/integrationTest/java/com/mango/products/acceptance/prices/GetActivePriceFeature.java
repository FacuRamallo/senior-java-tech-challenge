package com.mango.products.acceptance.prices;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mango.products.IntegrationTestBase;
import com.mango.products.prices.domain.Currency;
import com.mango.products.prices.domain.Description;
import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Money;
import com.mango.products.prices.domain.Name;
import com.mango.products.prices.domain.Price;
import com.mango.products.prices.domain.Product;
import com.mango.products.prices.domain.ValidityPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public abstract class GetActivePriceFeature extends IntegrationTestBase {

  @Test
  void shouldGetActivePriceOnDateSuccessfully() throws Exception {
    var productId = generateUUIDv7();
    var product =
        Product.create(
            new Id(productId),
            new Name("Zapatillas de correr"),
            new Description("Edición especial amortiguada"));
    productRepository.save(product);

    var priceId = generateUUIDv7();
    var price =
        Price.create(
            new Id(priceId),
            new Id(productId),
            new Money(new BigDecimal("149.99"), Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)));
    priceRepository.save(price);

    String expectedResponseBody =
        """
        {
          "value": 149.99,
          "currency": "EUR"
        }
        """;

    mockMvc
        .perform(
            get("/products/" + productId + "/prices")
                .param("date", "2024-03-15")
                .param("currency", "EUR")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedResponseBody));
  }
}
