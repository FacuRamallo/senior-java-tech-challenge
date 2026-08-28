package com.mango.products.infrastructure.configuration;

import com.mango.products.application.AddPriceToProductUseCase;
import com.mango.products.application.CreateProductUseCase;
import com.mango.products.application.UpdatePriceUseCase;
import com.mango.products.domain.IdGenerator;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ProductRepository;
import java.time.Clock;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean(initMethod = "migrate")
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration", "filesystem:/app/db/migration")
        .load();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public CreateProductUseCase createProductUseCase(
      ProductRepository productRepository, IdGenerator idGenerator) {
    return new CreateProductUseCase(productRepository, idGenerator);
  }

  @Bean
  public AddPriceToProductUseCase addPriceToProductUseCase(PriceRepository priceRepository) {
    return new AddPriceToProductUseCase(priceRepository);
  }

  @Bean
  public UpdatePriceUseCase updatePriceUseCase(PriceRepository priceRepository, Clock clock) {
    return new UpdatePriceUseCase(priceRepository, clock);
  }
}
