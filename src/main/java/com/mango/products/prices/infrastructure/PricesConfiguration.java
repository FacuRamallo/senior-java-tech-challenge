package com.mango.products.prices.infrastructure;

import com.mango.products.prices.application.AddPriceToProductUseCase;
import com.mango.products.prices.application.CreateProductUseCase;
import com.mango.products.prices.application.GetActivePriceUseCase;
import com.mango.products.prices.application.GetPriceHistoryUseCase;
import com.mango.products.prices.domain.PriceRepository;
import com.mango.products.prices.domain.ProductRepository;
import java.time.Clock;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricesConfiguration {

  @Bean(initMethod = "migrate")
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public CreateProductUseCase createProductUseCase(ProductRepository productRepository) {
    return new CreateProductUseCase(productRepository);
  }

  @Bean
  public AddPriceToProductUseCase addPriceToProductUseCase(PriceRepository priceRepository) {
    return new AddPriceToProductUseCase(priceRepository);
  }

  @Bean
  public GetActivePriceUseCase getActivePriceUseCase(PriceRepository priceRepository) {
    return new GetActivePriceUseCase(priceRepository);
  }

  @Bean
  public GetPriceHistoryUseCase getPriceHistoryUseCase(PriceRepository priceRepository) {
    return new GetPriceHistoryUseCase(priceRepository);
  }
}
