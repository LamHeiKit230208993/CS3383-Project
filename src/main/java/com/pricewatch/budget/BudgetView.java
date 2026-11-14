package com.pricewatch.budget;

import java.math.BigDecimal;
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
}
