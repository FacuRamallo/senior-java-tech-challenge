package com.mango.products.infrastructure.controller.readmode;

import java.util.List;

public record GetPriceHistoryResponse(
    String next, String previous, List<PriceHistoryItemResponse> prices) {}
