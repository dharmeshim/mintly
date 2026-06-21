package com.familyexpense.tracker.analytics;

import java.math.BigDecimal;

public class ItemSpendDto {

    private String itemName;
    private BigDecimal totalAmount;

    public ItemSpendDto() {
    }

    public ItemSpendDto(String itemName, BigDecimal totalAmount) {
        this.itemName = itemName;
        this.totalAmount = totalAmount;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
