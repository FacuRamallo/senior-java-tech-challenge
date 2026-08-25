package com.mango.products.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPriceToProductCommand(
    String priceId,
    String productId,
    BigDecimal amount,
    String currency,
    LocalDate initDate,
    LocalDate endDate) {}
