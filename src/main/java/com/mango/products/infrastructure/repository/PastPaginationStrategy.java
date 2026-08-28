package com.mango.products.infrastructure.repository;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class PastPaginationStrategy implements PaginationStrategy {

  private final LocalDate from;
  private final int lookAheadLimit;

  public PastPaginationStrategy(LocalDate from, int lookAheadLimit) {
    this.from = from;
    this.lookAheadLimit = lookAheadLimit;
  }

  @Override
  public String getSql() {
    return """
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
          AND (:from::date IS NULL OR init_date < :from::date)
        ORDER BY init_date DESC
        LIMIT :pageSize
        """;
  }

  @Override
  public void bindParameters(MapSqlParameterSource params) {
    params.addValue("from", from != null ? Date.valueOf(from) : null, Types.DATE);
    params.addValue("pageSize", lookAheadLimit);
  }

  @Override
  public CursorDirection getDirection() {
    return CursorDirection.PAST;
  }
}
