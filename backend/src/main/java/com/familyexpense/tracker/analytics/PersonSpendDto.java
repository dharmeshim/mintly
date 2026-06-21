package com.familyexpense.tracker.analytics;

import java.math.BigDecimal;

public record PersonSpendDto(
        Long profileId,
        String profileName,
        BigDecimal totalAmount
) {}
