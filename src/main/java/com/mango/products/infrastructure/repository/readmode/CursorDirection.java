package com.mango.products.infrastructure.repository.readmode;

public enum CursorDirection {
  FUTURE,
  PAST;

  public static CursorDirection from(String raw, SortOrder sortOrder) {
    if (raw == null || raw.isBlank()) {
      return defaultCursorDirectionFor(sortOrder);
    }
    try {
      return CursorDirection.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Cursor direction must be one of: FUTURE, PAST");
    }
  }

  public static CursorDirection defaultCursorDirectionFor(SortOrder sortOrder) {
    return switch (sortOrder) {
      case SortOrder.ASC -> FUTURE;
      case SortOrder.DESC -> PAST;
    };
  }

  public CursorDirection opposite() {
    return this == FUTURE ? PAST : FUTURE;
  }
}
