package com.familyexpense.tracker.analytics;

import com.familyexpense.tracker.purchase.Purchase;
import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
    BigDecimal thisMonthTotal,
    CostPerHeadDto costPerHead,
    List<ItemSpendDto> itemSpend,
    List<Purchase> recentPurchases,
    List<MonthlyTrendDto> monthlyTrend
) {
    public record MonthlyTrendDto(
        String month,
        BigDecimal totalAmount,
        long activeMembers,
        BigDecimal perHeadAmount
    ) {}
}
