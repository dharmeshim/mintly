package com.familyexpense.tracker.analytics;

import java.math.BigDecimal;

public record CostPerHeadDto(
    BigDecimal totalAmount,
    long activeMembers,
    BigDecimal perHeadAmount
) {}
