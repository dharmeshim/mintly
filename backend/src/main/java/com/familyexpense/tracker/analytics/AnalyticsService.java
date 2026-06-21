package com.familyexpense.tracker.analytics;

import com.familyexpense.tracker.category.Category;
import com.familyexpense.tracker.item.ItemService;
import com.familyexpense.tracker.profile.ProfileRepository;
import com.familyexpense.tracker.purchase.Purchase;
import com.familyexpense.tracker.purchase.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final PurchaseRepository purchaseRepository;
    private final ProfileRepository profileRepository;
    private final ItemService itemService;

    public AnalyticsService(
            PurchaseRepository purchaseRepository,
            ProfileRepository profileRepository,
            ItemService itemService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.profileRepository = profileRepository;
        this.itemService = itemService;
    }

    public PriceHistoryDto getPriceHistory(Long itemId) {
        itemService.getItemById(itemId);

        List<Purchase> purchases = purchaseRepository.findByItemIdOrderByPurchaseDateAsc(itemId);
        List<PriceHistoryDto.PriceHistoryPoint> points = purchases.stream()
                .map(p -> new PriceHistoryDto.PriceHistoryPoint(
                        p.getPurchaseDate(),
                        p.getRate(),
                        p.getQuantity(),
                        p.getProfile().getName()
                ))
                .collect(Collectors.toList());

        BigDecimal latestRate = BigDecimal.ZERO;
        BigDecimal previousRate = BigDecimal.ZERO;
        BigDecimal deltaRate = BigDecimal.ZERO;
        BigDecimal deltaPercent = BigDecimal.ZERO;

        int size = purchases.size();
        if (size > 0) {
            latestRate = purchases.get(size - 1).getRate();
            if (size > 1) {
                previousRate = purchases.get(size - 2).getRate();
                deltaRate = latestRate.subtract(previousRate);
                if (previousRate.compareTo(BigDecimal.ZERO) > 0) {
                    deltaPercent = deltaRate.multiply(new BigDecimal("100"))
                            .divide(previousRate, 2, RoundingMode.HALF_UP);
                }
            }
        }

        return new PriceHistoryDto(points, latestRate, previousRate, deltaRate, deltaPercent);
    }

    public BuyingIntervalDto getBuyingInterval(Long itemId) {
        itemService.getItemById(itemId);

        List<Purchase> purchases = purchaseRepository.findByItemIdOrderByPurchaseDateAsc(itemId);
        List<BuyingIntervalDto.BuyingIntervalPoint> intervals = new ArrayList<>();

        long totalDays = 0;
        int intervalCount = 0;

        for (int i = 1; i < purchases.size(); i++) {
            LocalDate prevDate = purchases.get(i - 1).getPurchaseDate();
            LocalDate currDate = purchases.get(i).getPurchaseDate();
            long days = ChronoUnit.DAYS.between(prevDate, currDate);
            intervals.add(new BuyingIntervalDto.BuyingIntervalPoint(currDate, days));
            totalDays += days;
            intervalCount++;
        }

        Double averageIntervalDays = intervalCount > 0 ? (double) totalDays / intervalCount : null;
        return new BuyingIntervalDto(intervals, averageIntervalDays);
    }

    public List<CategorySpendDto> getCategorySpend(String monthStr) {
        YearMonth ym = parseYearMonth(monthStr);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Purchase> purchases = purchaseRepository.findWithFilters(start, end, null, null, null);

        Map<Category, BigDecimal> spendMap = purchases.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getItem().getCategory(),
                        Collectors.reducing(BigDecimal.ZERO, Purchase::getTotalAmount, BigDecimal::add)
                ));

        return spendMap.entrySet().stream()
                .map(e -> new CategorySpendDto(
                        e.getKey().getId(),
                        e.getKey().getName(),
                        e.getKey().getColor(),
                        e.getValue()
                ))
                .sorted(Comparator.comparing(CategorySpendDto::totalAmount).reversed())
                .collect(Collectors.toList());
    }

    public CostPerHeadDto getCostPerHead(String monthStr) {
        YearMonth ym = parseYearMonth(monthStr);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Purchase> purchases = purchaseRepository.findWithFilters(start, end, null, null, null);
        BigDecimal total = purchases.stream()
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeMembers = profileRepository.findByActiveTrue().size();
        BigDecimal perHead = BigDecimal.ZERO;
        if (activeMembers > 0) {
            perHead = total.divide(new BigDecimal(activeMembers), 2, RoundingMode.HALF_UP);
        }

        return new CostPerHeadDto(total, activeMembers, perHead);
    }

    public DashboardSummaryDto getDashboardSummary(String monthStr) {
        YearMonth currentYm = parseYearMonth(monthStr);

        // 1. This month totals and per-head split
        CostPerHeadDto costPerHead = getCostPerHead(currentYm.toString());

        // 2. Category spending breakdown
        List<CategorySpendDto> categorySpend = getCategorySpend(currentYm.toString());

        // 3. Recent 5 purchases overall
        List<Purchase> recentPurchases = purchaseRepository.findWithFilters(null, null, null, null, null)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        // 4. Last 6 months trend (ending with target month)
        List<DashboardSummaryDto.MonthlyTrendDto> trend = new ArrayList<>();
        long activeMembers = profileRepository.findByActiveTrue().size();

        for (int i = 5; i >= 0; i--) {
            YearMonth targetYm = currentYm.minusMonths(i);
            LocalDate start = targetYm.atDay(1);
            LocalDate end = targetYm.atEndOfMonth();

            List<Purchase> ymPurchases = purchaseRepository.findWithFilters(start, end, null, null, null);
            BigDecimal total = ymPurchases.stream()
                    .map(Purchase::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal perHead = BigDecimal.ZERO;
            if (activeMembers > 0) {
                perHead = total.divide(new BigDecimal(activeMembers), 2, RoundingMode.HALF_UP);
            }

            trend.add(new DashboardSummaryDto.MonthlyTrendDto(
                    targetYm.toString(),
                    total,
                    activeMembers,
                    perHead
            ));
        }

        return new DashboardSummaryDto(
                costPerHead.totalAmount(),
                costPerHead,
                categorySpend,
                recentPurchases,
                trend
        );
    }

    private YearMonth parseYearMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(monthStr);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }
}
