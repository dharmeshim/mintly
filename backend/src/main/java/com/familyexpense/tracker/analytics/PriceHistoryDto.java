package com.familyexpense.tracker.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PriceHistoryDto(
    List<PriceHistoryPoint> history,
    BigDecimal latestRate,
    BigDecimal previousRate,
    BigDecimal deltaRate,
    BigDecimal deltaPercent
) {
    public record PriceHistoryPoint(
        LocalDate date,
        BigDecimal rate,
        BigDecimal quantity,
        String loggedBy
    ) {}
}
