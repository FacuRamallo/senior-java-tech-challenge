package com.mango.products.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

public abstract class DeletePriceFeature extends IntegrationTestBase {

  @Test
  void shouldDeletePriceSuccessfully() throws Exception {
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

    mockMvc
        .perform(delete("/products/" + productId + "/prices/" + priceId))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
  }
}
