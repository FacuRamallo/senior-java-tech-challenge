package com.mango.products.acceptance.prices;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mango.products.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public abstract class CreateProductFeature extends IntegrationTestBase {

  @Test
  void shouldCreateProductSuccessfully() throws Exception {
    String id = generateUUIDv7().toString();
    String requestBody =
        """
        {
          "id": "%s",
          "name": "Zapatillas deportivas",
          "description": "Modelo 2025 edición limitada"
        }
        """
            .formatted(id);

    mockMvc
        .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/products/" + id))
        .andExpect(content().string(""));
  }
}
