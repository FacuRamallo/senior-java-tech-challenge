package com.mango.products.prices.infrastructure;

import com.mango.products.prices.application.CreateProductUseCase;
import com.mango.products.prices.domain.ProductRepository;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricesConfiguration {

  @Bean(initMethod = "migrate")
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure().dataSource(dataSource).load();
  }

  @Bean
  public CreateProductUseCase createProductUseCase(ProductRepository productRepository) {
    return new CreateProductUseCase(productRepository);
  }
}
