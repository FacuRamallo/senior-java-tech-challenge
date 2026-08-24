package com.mango.products.prices.infrastructure;

import com.mango.products.prices.domain.Product;
import com.mango.products.prices.domain.ProductRepository;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgreSqlProductRepository implements ProductRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PostgreSqlProductRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(Product product) {
    String sql = "INSERT INTO product (id, name, description) VALUES (:id, :name, :description)";
    var params =
        Map.of(
            "id", product.getId().value(),
            "name", product.getName().value(),
            "description", product.getDescription().value());
    jdbcTemplate.update(sql, params);
  }
}
