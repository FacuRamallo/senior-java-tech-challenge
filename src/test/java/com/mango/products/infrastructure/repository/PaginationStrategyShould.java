package com.mango.products.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mango.products.infrastructure.controller.readmode.PageSize;
import com.mango.products.infrastructure.repository.readmode.PaginationSortingStrategyFactory;
import com.mango.products.infrastructure.repository.readmode.SortOrder;
import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

class PaginationStrategyShould {

  @Test
  void futurePaginationStrategyFetchesPricesWithDatesAfterCursorOrderedAscending() {
    var cursor = LocalDate.of(2024, 7, 1);
    int lookAheadLimit = new PageSize(20).lookAheadLimit();
    var strategy =
        PaginationSortingStrategyFactory.create("FUTURE", SortOrder.ASC, cursor, lookAheadLimit);

    var params = new MapSqlParameterSource();
    strategy.bindParameters(params);

    assertThat(strategy.getSql()).contains("ORDER BY init_date ASC");
    assertThat(strategy.getSql()).contains("(:from::date IS NULL OR init_date > :from::date)");
    assertThat(params.getValue("from")).isEqualTo(Date.valueOf(cursor));
    assertThat(params.getValue("pageSize")).isEqualTo(lookAheadLimit);
  }

  @Test
  void pastPaginationStrategyFetchesPricesWithDatesBeforeCursorOrderedDescending() {
    var cursor = LocalDate.of(2024, 1, 1);
    int lookAheadLimit = new PageSize(20).lookAheadLimit();
    var strategy =
        PaginationSortingStrategyFactory.create("PAST", SortOrder.DESC, cursor, lookAheadLimit);

    var params = new MapSqlParameterSource();
    strategy.bindParameters(params);

    assertThat(strategy.getSql()).contains("ORDER BY init_date DESC");
    assertThat(strategy.getSql()).contains("(:from::date IS NULL OR init_date < :from::date)");
    assertThat(params.getValue("from")).isEqualTo(Date.valueOf(cursor));
    assertThat(params.getValue("pageSize")).isEqualTo(lookAheadLimit);
  }
}
