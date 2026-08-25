package com.mango.products;

import com.mango.products.acceptance.AddPriceFeature;
import com.mango.products.acceptance.CreateProductFeature;
import com.mango.products.acceptance.GetActivePriceFeature;
import com.mango.products.acceptance.GetPriceHistoryFeature;
import com.mango.products.infrastructure.helper.DockerComposeHelper;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IntegrationTestSuite {

  private static final ComposeContainer composeContainer;

  static {
    composeContainer = DockerComposeHelper.create();
    composeContainer.start();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    DockerComposeHelper.setDynamicProperties(composeContainer, registry);
  }

  @Nested
  class Application extends ApplicationTests {}

  @Nested
  class CreateProduct extends CreateProductFeature {}

  @Nested
  class AddPrice extends AddPriceFeature {}

  @Nested
  class GetActivePrice extends GetActivePriceFeature {}

  @Nested
  class GetPriceHistory extends GetPriceHistoryFeature {}
}
