package com.mango.products.infrastructure.repository;

import com.mango.products.domain.DomainException.DuplicateProductNameException;
import com.mango.products.domain.Id;
import com.mango.products.domain.Name;
import com.mango.products.domain.Product;
import com.mango.products.domain.ProductRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
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
    try {
      jdbcTemplate.update(sql, params);
    } catch (DataIntegrityViolationException ex) {
      if (isDuplicateNameViolation(ex)) {
        throw new DuplicateProductNameException(product.getName());
      }
      throw ex;
    }
  }

  @Override
  public Optional<Id> findConflictingProductId(Name name) {
    String sql = "SELECT id FROM product WHERE LOWER(name) = LOWER(:name) LIMIT 1";
    var results =
        jdbcTemplate.query(
            sql, Map.of("name", name.value()), (rs, _) -> rs.getObject("id", UUID.class));
    return results.stream().findFirst().map(Id::new);
  }

  private boolean isDuplicateNameViolation(DataIntegrityViolationException ex) {
    String message = ex.getMessage();
    return message != null && message.contains("uk_product_name_lower");
  }
}
