package com.mango.products.prices.infrastructure;

import com.mango.products.prices.domain.Price;
import com.mango.products.prices.domain.PriceRepository;
import java.sql.Date;
import java.sql.Types;
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
            .addValue("priceCurrency", price.getMoney().currency().getCurrencyCode())
            .addValue("initDate", Date.valueOf(price.getValidityPeriod().initDate()))
            .addValue(
                "endDate",
                price.getValidityPeriod().endDate() != null
                    ? Date.valueOf(price.getValidityPeriod().endDate())
                    : null,
                Types.DATE);

    jdbcTemplate.update(sql, params);
  }
}
