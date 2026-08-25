package com.mango.products;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

public abstract class ApplicationTests extends IntegrationTestBase {

  @Test
  void healthEndpointShouldReturnOk() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void baseEndpointShouldReturnNotFoundOrOk() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().is4xxClientError());
  }
}
