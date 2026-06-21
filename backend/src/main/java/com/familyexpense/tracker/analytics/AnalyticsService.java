package com.familyexpense.tracker.analytics;

import com.familyexpense.tracker.item.Item;
import com.familyexpense.tracker.item.ItemService;
import com.familyexpense.tracker.profile.Profile;
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

    public List<ItemSpendDto> getItemSpend(String periodStr) {
        LocalDate[] range = parsePeriod(periodStr);
        List<Purchase> purchases = purchaseRepository.findWithFilters(range[0], range[1], null, null);

        Map<String, BigDecimal> spendMap = purchases.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getItem().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Purchase::getTotalAmount, BigDecimal::add)
                ));

        return spendMap.entrySet().stream()
                .map(e -> new ItemSpendDto(
                        e.getKey(),
                        e.getValue()
                ))
                .sorted(Comparator.comparing(ItemSpendDto::getTotalAmount).reversed())
                .collect(Collectors.toList());
    }

    public List<PersonSpendDto> getPersonSpend(String periodStr) {
        LocalDate[] range = parsePeriod(periodStr);
        List<Purchase> purchases = purchaseRepository.findWithFilters(range[0], range[1], null, null);

        Map<Profile, BigDecimal> spendMap = purchases.stream()
                .collect(Collectors.groupingBy(
                        Purchase::getProfile,
                        Collectors.reducing(BigDecimal.ZERO, Purchase::getTotalAmount, BigDecimal::add)
                ));

        return spendMap.entrySet().stream()
                .map(e -> new PersonSpendDto(
                        e.getKey().getId(),
                        e.getKey().getName(),
                        e.getValue()
                ))
                .sorted(Comparator.comparing(PersonSpendDto::totalAmount).reversed())
                .collect(Collectors.toList());
    }

    public List<PriceChangeDto> getPriceChanges(String periodStr) {
        LocalDate[] range = parsePeriod(periodStr);
        List<Purchase> purchasesInPeriod = purchaseRepository.findWithFilters(range[0], range[1], null, null);

        // Find unique items bought in this period
        Set<Long> itemIds = purchasesInPeriod.stream()
                .map(p -> p.getItem().getId())
                .collect(Collectors.toSet());

        List<PriceChangeDto> priceChanges = new ArrayList<>();

        for (Long itemId : itemIds) {
            PriceHistoryDto history = getPriceHistory(itemId);
            if (history.history().size() > 1 && history.deltaRate().compareTo(BigDecimal.ZERO) != 0) {
                // Ensure the latest purchase actually happened in our period
                LocalDate lastPurchaseDate = history.history().get(history.history().size() - 1).date();
                if (!lastPurchaseDate.isBefore(range[0]) && !lastPurchaseDate.isAfter(range[1])) {
                    Item item = itemService.getItemById(itemId);
                    LocalDate prevDate = history.history().get(history.history().size() - 2).date();
                    priceChanges.add(new PriceChangeDto(
                            itemId,
                            item.getName(),
                            history.latestRate(),
                            history.previousRate(),
                            history.deltaRate(),
                            history.deltaPercent(),
                            lastPurchaseDate,
                            prevDate
                    ));
                }
            }
        }

        // Sort by biggest absolute percentage change
        priceChanges.sort((a, b) -> b.deltaPercent().abs().compareTo(a.deltaPercent().abs()));
        return priceChanges;
    }

    public CostPerHeadDto getCostPerHead(String periodStr) {
        LocalDate[] range = parsePeriod(periodStr);
        List<Purchase> purchases = purchaseRepository.findWithFilters(range[0], range[1], null, null);
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
        YearMonth currentYm;
        try {
            currentYm = YearMonth.parse(monthStr);
        } catch (Exception e) {
            currentYm = YearMonth.now();
        }

        // 1. This month totals and per-head split
        CostPerHeadDto costPerHead = getCostPerHead(currentYm.toString());

        // 2. Item spending breakdown
        List<ItemSpendDto> itemSpend = getItemSpend(currentYm.toString());

        // 3. Recent 5 purchases overall
        List<Purchase> recentPurchases = purchaseRepository.findWithFilters(null, null, null, null)
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

            List<Purchase> ymPurchases = purchaseRepository.findWithFilters(start, end, null, null);
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
                itemSpend,
                recentPurchases,
                trend
        );
    }

    private LocalDate[] parsePeriod(String periodStr) {
        if (periodStr == null || periodStr.isBlank()) {
            YearMonth ym = YearMonth.now();
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
        if (periodStr.length() == 4) { // YYYY
            try {
                int year = Integer.parseInt(periodStr);
                return new LocalDate[]{LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)};
            } catch (Exception e) {
                YearMonth ym = YearMonth.now();
                return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            }
        }
        try {
            YearMonth ym = YearMonth.parse(periodStr);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        } catch (Exception e) {
            YearMonth ym = YearMonth.now();
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
    }
}
