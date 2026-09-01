package com.mango.products.infrastructure.repository.readmode;

public enum SortOrder {
  ASC,
  DESC;

  public static SortOrder from(String raw) {
    if (raw == null || raw.isBlank()) {
      return DESC;
    }
    try {
      return SortOrder.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Sort order must be one of: ASC, DESC");
    }
  }

  public boolean requiresReversal(CursorDirection direction) {
    return switch (this) {
      case ASC -> CursorDirection.FUTURE != direction;
      case DESC -> CursorDirection.PAST != direction;
    };
  }
}
