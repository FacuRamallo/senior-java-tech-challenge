package com.mango.products.infrastructure.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public interface PaginationStrategy {

  String getSql();

  void bindParameters(MapSqlParameterSource params);

  CursorDirection getDirection();
}
