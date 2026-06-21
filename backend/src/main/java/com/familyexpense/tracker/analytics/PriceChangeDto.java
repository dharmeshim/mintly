package com.familyexpense.tracker.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceChangeDto(
        Long itemId,
        String itemName,
        BigDecimal currentRate,
        BigDecimal previousRate,
        BigDecimal deltaRate,
        BigDecimal deltaPercent,
        LocalDate currentPurchaseDate,
        LocalDate previousPurchaseDate
) {}
