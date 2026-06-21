package com.familyexpense.tracker.analytics;

import java.time.LocalDate;
import java.util.List;

public record BuyingIntervalDto(
    List<BuyingIntervalPoint> intervals,
    Double averageIntervalDays
) {
    public record BuyingIntervalPoint(
        LocalDate date,
        Long daysSinceLast
    ) {}
}
