package com.mango.products;

import com.fasterxml.uuid.Generators;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ProductRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public abstract class IntegrationTestBase {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ProductRepository productRepository;
  @Autowired protected PriceRepository priceRepository;

  protected static UUID generateUUIDv7() {
    return Generators.timeBasedEpochGenerator().generate();
  }
}
