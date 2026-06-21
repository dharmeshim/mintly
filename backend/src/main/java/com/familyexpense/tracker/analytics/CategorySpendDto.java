package com.familyexpense.tracker.analytics;

import java.math.BigDecimal;

public record CategorySpendDto(
    Long categoryId,
    String categoryName,
    String color,
    BigDecimal totalAmount
) {}
