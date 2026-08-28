package com.mango.products.infrastructure.repository;

import com.mango.products.domain.Currency;
import com.mango.products.domain.Id;
import com.mango.products.domain.Money;
import com.mango.products.domain.Price;
import com.mango.products.domain.ValidityPeriod;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PriceHistoryReader {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PriceHistoryReader(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Price> find(Id productId, Currency currency, PaginationStrategy condition) {
    var params =
        new MapSqlParameterSource()
            .addValue("productId", productId.value())
            .addValue("priceCurrency", currency.value());

    condition.bindParameters(params);

    return jdbcTemplate.query(
        condition.getSql(),
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
  }
}
