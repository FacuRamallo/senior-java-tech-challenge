package com.mango.products.infrastructure.repository;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.PriceRepository;
import com.mango.products.domain.ValidityPeriod;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    jdbcTemplate.update(sql, params);
  }

  @Override
  public Optional<Price> findActivePrice(Id productId, LocalDate date, Currency currency) {
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
        WHERE product_id = :productId
          AND price_currency = :priceCurrency
          AND daterange(init_date, coalesce(end_date, 'infinity'), '[]') @> :date::date
        LIMIT 1
        """;

    var params =
        new MapSqlParameterSource()
            .addValue("productId", productId.value())
            .addValue("priceCurrency", currency.value())
            .addValue("date", Date.valueOf(date));

    List<Price> results =
        jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> {
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
  public List<Price> findPriceHistory(Id productId, Currency currency) {
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
        WHERE product_id = :productId
          AND price_currency = :priceCurrency
        ORDER BY init_date ASC
        """;

    var params =
        new MapSqlParameterSource()
            .addValue("productId", productId.value())
            .addValue("priceCurrency", currency.value());

    return jdbcTemplate.query(
        sql,
        params,
        (rs, rowNum) -> {
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
  }
}
