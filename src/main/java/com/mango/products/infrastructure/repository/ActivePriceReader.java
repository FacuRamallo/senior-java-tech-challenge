package com.mango.products.infrastructure.repository;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.ValidityPeriod;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ActivePriceReader {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ActivePriceReader(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

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
}
