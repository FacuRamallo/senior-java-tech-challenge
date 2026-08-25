package com.mango.products.acceptance.prices;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.uuid.Generators;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public abstract class AddPriceFeature {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldAddPriceToProductSuccessfully() throws Exception {
    String productId = generateUUIDv7().toString();
    String productRequestBody =
        """
        {
          "id": "%s",
          "name": "Zapatillas deportivas",
          "description": "Modelo 2025 edición limitada"
        }
        """
            .formatted(productId);

    mockMvc
        .perform(
            post("/products").contentType(MediaType.APPLICATION_JSON).content(productRequestBody))
        .andExpect(status().isCreated());

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
                    matchesPattern(
                        "^/products/"
                            + productId
                            + "/prices/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-7[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")))
        .andExpect(content().string(""));
  }

  private static UUID generateUUIDv7() {
    return Generators.timeBasedEpochGenerator().generate();
  }
}
