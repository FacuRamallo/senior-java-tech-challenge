package com.mango.products;

import com.fasterxml.uuid.Generators;
import com.mango.products.infrastructure.repository.PriceRepositoryForTest;
import com.mango.products.infrastructure.repository.ProductRepositoryForTest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public abstract class IntegrationTestBase {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ProductRepositoryForTest productRepository;
  @Autowired protected PriceRepositoryForTest priceRepository;

  protected static UUID generateUUIDv7() {
    return Generators.timeBasedEpochGenerator().generate();
  }
}
