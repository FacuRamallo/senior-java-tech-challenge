package com.mango.products.infrastructure.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePriceRequest(
    BigDecimal value, String currency, LocalDate initDate, LocalDate endDate) {}
