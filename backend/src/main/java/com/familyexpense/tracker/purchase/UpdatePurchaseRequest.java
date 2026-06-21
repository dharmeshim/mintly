package com.familyexpense.tracker.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePurchaseRequest(
    Long itemId,
    String itemName,
    String unit,
    BigDecimal quantity,
    BigDecimal rate,
    LocalDate purchaseDate,
    String shop,
    String paymentMode,
    String notes
) {}
