package com.mango.products.infrastructure.repository;

import com.mango.products.domain.Currency;
import com.mango.products.domain.DomainException.PriceValidityOverlapException;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.sql.Date;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgreSqlPriceRepository implements PriceRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PostgreSqlPriceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(Price price) {
    String sql =
        """
        INSERT INTO product_prices (
            id,
            product_id,
            price_amount,
            price_currency,
            init_date,
            end_date
        ) VALUES (
            :id,
            :productId,
            :priceAmount,
            :priceCurrency,
            :initDate,
            :endDate
        )
        """;

    var params =
        new MapSqlParameterSource()
            .addValue("id", price.getId().value())
            .addValue("productId", price.getProductId().value())
            .addValue("priceAmount", price.getMoney().amount())
            .addValue("priceCurrency", price.getMoney().currency().value())
            .addValue("initDate", Date.valueOf(price.getValidityPeriod().initDate()))
            .addValue(
                "endDate",
                price.getValidityPeriod().endDate() != null
                    ? Date.valueOf(price.getValidityPeriod().endDate())
                    : null,
                Types.DATE);

    try {
      jdbcTemplate.update(sql, params);
    } catch (DataIntegrityViolationException ex) {
      if (isPriceOverlapViolation(ex)) {
        throw new PriceValidityOverlapException();
      }
      throw ex;
    }
  }

  @Override
  public Optional<Price> findById(Id priceId, Id productId) {
    String sql =
        """
        SELECT
            id,
            product_id,
            price_amount,
            price_currency,
            init_date,
            end_date
        FROM product_prices
        WHERE id = :id AND product_id = :productId
        LIMIT 1
        """;

    var params =
        new MapSqlParameterSource()
            .addValue("id", priceId.value())
            .addValue("productId", productId.value());

    List<Price> results =
        jdbcTemplate.query(
            sql,
            params,
            (rs, _) -> {
              var id = new Id(rs.getObject("id", UUID.class));
              var pId = new Id(rs.getObject("product_id", UUID.class));
              var amount = rs.getBigDecimal("price_amount");
              var curr = Currency.from(rs.getString("price_currency"));
              var initDate = rs.getDate("init_date").toLocalDate();
              var endDateSql = rs.getDate("end_date");
              var endDate = endDateSql != null ? endDateSql.toLocalDate() : null;
              return Price.create(
                  id, pId, new Money(amount, curr), new ValidityPeriod(initDate, endDate));
            });

    return results.stream().findFirst();
  }

  @Override
  public boolean update(Price price) {
    String sql =
        """
        UPDATE product_prices
        SET
            price_amount = :priceAmount,
            price_currency = :priceCurrency,
            init_date = :initDate,
            end_date = :endDate
        WHERE id = :id AND product_id = :productId
        """;

    var params =
        new MapSqlParameterSource()
            .addValue("id", price.getId().value())
            .addValue("productId", price.getProductId().value())
            .addValue("priceAmount", price.getMoney().amount())
            .addValue("priceCurrency", price.getMoney().currency().value())
            .addValue("initDate", Date.valueOf(price.getValidityPeriod().initDate()))
            .addValue(
                "endDate",
                price.getValidityPeriod().endDate() != null
                    ? Date.valueOf(price.getValidityPeriod().endDate())
                    : null,
                Types.DATE);

    try {
      int rows = jdbcTemplate.update(sql, params);
      return rows > 0;
    } catch (DataIntegrityViolationException ex) {
      if (isPriceOverlapViolation(ex)) {
        throw new PriceValidityOverlapException();
      }
      throw ex;
    }
  }

  @Override
  public Optional<Price> findLatestPrice(Id productId, Currency currency) {
    String sql =
        """
        SELECT
            id,
            product_id,
            price_amount,
            price_currency,
            init_date,
            end_date
        FROM product_prices
        WHERE product_id = :productId AND price_currency = :priceCurrency
        ORDER BY init_date DESC
        LIMIT 1
        """;

    var params =
        new MapSqlParameterSource()
            .addValue("productId", productId.value())
            .addValue("priceCurrency", currency.value());

    List<Price> results =
        jdbcTemplate.query(
            sql,
            params,
            (rs, _) -> {
              var id = new Id(rs.getObject("id", UUID.class));
              var pId = new Id(rs.getObject("product_id", UUID.class));
              var amount = rs.getBigDecimal("price_amount");
              var curr = Currency.from(rs.getString("price_currency"));
              var initDate = rs.getDate("init_date").toLocalDate();
              var endDateSql = rs.getDate("end_date");
              var endDate = endDateSql != null ? endDateSql.toLocalDate() : null;
              return Price.create(
                  id, pId, new Money(amount, curr), new ValidityPeriod(initDate, endDate));
            });

    return results.stream().findFirst();
  }

  private boolean isPriceOverlapViolation(DataIntegrityViolationException ex) {
    String message = ex.getMessage();
    return message != null
        && (message.contains("ex_product_currency_validity") || message.contains("23P01"));
  }
}
