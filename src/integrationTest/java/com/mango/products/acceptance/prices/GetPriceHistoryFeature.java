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

public abstract class GetPriceHistoryFeature extends IntegrationTestBase {

  @Test
  void shouldGetPriceHistoryChronologicallySuccessfully() throws Exception {
    var productId = generateUUIDv7();
    var product =
        Product.create(
            new Id(productId),
            new Name("Zapatillas de running"),
            new Description("Modelo profesional amortiguado"));
    productRepository.save(product);

    var priceId1 = generateUUIDv7();
    var price1 =
        Price.create(
            new Id(priceId1),
            new Id(productId),
            new Money(new BigDecimal("99.99"), Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)));
    priceRepository.save(price1);

    var priceId2 = generateUUIDv7();
    var price2 =
        Price.create(
            new Id(priceId2),
            new Id(productId),
            new Money(new BigDecimal("149.99"), Currency.DEFAULT),
            new ValidityPeriod(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31)));
    priceRepository.save(price2);

    String expectedResponseBody =
        """
        [
          {
            "id": "%s",
            "value": 99.99,
            "currency": "EUR",
            "initDate": "2024-01-01",
            "endDate": "2024-06-30"
          },
          {
            "id": "%s",
            "value": 149.99,
            "currency": "EUR",
            "initDate": "2024-07-01",
            "endDate": "2024-12-31"
          }
        ]
        """
            .formatted(priceId1, priceId2);

    mockMvc
        .perform(
            get("/products/" + productId + "/prices")
                .param("currency", "EUR")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedResponseBody));
  }
}
