package com.mango.products.acceptance.prices;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mango.products.IntegrationTestBase;
import com.mango.products.prices.domain.Description;
import com.mango.products.prices.domain.Id;
import com.mango.products.prices.domain.Name;
import com.mango.products.prices.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public abstract class AddPriceFeature extends IntegrationTestBase {

  private static final String UUID_V7_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-7[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";

  @Test
  void shouldAddPriceToProductSuccessfully() throws Exception {
    var productId = generateUUIDv7();
    var product =
        Product.create(
            new Id(productId),
            new Name("Zapatillas deportivas"),
            new Description("Modelo 2025 edición limitada"));
    productRepository.save(product);

    String priceRequestBody =
        """
        {
          "value": 99.99,
          "initDate": "2024-01-01",
          "endDate": "2024-06-30"
        }
        """;

    mockMvc
        .perform(
            post("/products/" + productId + "/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(priceRequestBody))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    matchesPattern("^/products/" + productId + "/prices/" + UUID_V7_PATTERN + "$")))
        .andExpect(content().string(""));
  }
}
