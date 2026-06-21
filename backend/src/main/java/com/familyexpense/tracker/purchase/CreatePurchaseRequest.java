package com.familyexpense.tracker.purchase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseRequest(
    Long itemId,
    String itemName,
    Long categoryId,
    String categoryName,
    String categoryColor,
    String unit,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal rate,
    LocalDate purchaseDate,
    String shop,
    String paymentMode,
    String notes
) {}
