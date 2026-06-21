package com.familyexpense.tracker.analytics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/item/{itemId}/price-history")
    public ResponseEntity<PriceHistoryDto> getPriceHistory(@PathVariable Long itemId) {
        return ResponseEntity.ok(analyticsService.getPriceHistory(itemId));
    }

    @GetMapping("/item/{itemId}/buying-interval")
    public ResponseEntity<BuyingIntervalDto> getBuyingInterval(@PathVariable Long itemId) {
        return ResponseEntity.ok(analyticsService.getBuyingInterval(itemId));
    }

    @GetMapping("/category-spend")
    public ResponseEntity<List<CategorySpendDto>> getCategorySpend(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(analyticsService.getCategorySpend(month));
    }

    @GetMapping("/cost-per-head")
    public ResponseEntity<CostPerHeadDto> getCostPerHead(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(analyticsService.getCostPerHead(month));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(analyticsService.getDashboardSummary(month));
    }
}
