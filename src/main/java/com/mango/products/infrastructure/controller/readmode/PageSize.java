package com.mango.products.infrastructure.controller.readmode;

public record PageSize(Integer value) {

  private static final int MIN = 20;
  private static final int MAX = 100;

  public PageSize {
    if (value == null || value < MIN) {
      value = MIN;
    }
    if (value > MAX) {
      throw new IllegalArgumentException("Page size must not exceed " + MAX);
    }
  }

  public int lookAheadLimit() {
    return value + 1;
  }
}
