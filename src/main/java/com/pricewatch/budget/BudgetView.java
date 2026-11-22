package com.pricewatch.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record BudgetView(
        BudgetPeriod period,
        BigDecimal limitAmount,
        BigDecimal knownSubtotal,
        int unknownItemCount,
        List<CategoryTotal> categoryTotals,
        BudgetState state) {
    public BudgetView {
        categoryTotals = List.copyOf(categoryTotals);
    }

    public int progressPercent() {
        if (limitAmount == null || knownSubtotal.signum() == 0) return 0;
        if (knownSubtotal.compareTo(limitAmount) >= 0) return 100;
        return knownSubtotal.movePointRight(2).divide(limitAmount, 0, RoundingMode.DOWN).intValueExact();
    }

    public BigDecimal balance() {
        return limitAmount == null ? null : limitAmount.subtract(knownSubtotal);
    }
}
