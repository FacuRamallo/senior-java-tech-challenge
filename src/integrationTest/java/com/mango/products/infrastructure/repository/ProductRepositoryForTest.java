package com.mango.products.infrastructure.repository;

import com.mango.products.domain.Product;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryForTest {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ProductRepositoryForTest(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

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
