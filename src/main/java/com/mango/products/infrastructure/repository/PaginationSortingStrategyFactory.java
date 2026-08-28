package com.mango.products.infrastructure.repository;

import java.time.LocalDate;

public class PaginationSortingStrategyFactory {

  public static PaginationStrategy create(
      String rawCursorDirection, SortOrder sortOrder, LocalDate cursor, int lookAheadLimit) {
    CursorDirection direction = CursorDirection.from(rawCursorDirection, sortOrder);

    return switch (direction) {
      case FUTURE -> new FuturePaginationStrategy(cursor, lookAheadLimit);
      case PAST -> new PastPaginationStrategy(cursor, lookAheadLimit);
    };
  }
}
