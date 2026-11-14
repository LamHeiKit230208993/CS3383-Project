package com.pricewatch.budget;

import java.math.BigDecimal;
import java.util.List;

/** Planned-cost view for My list. Never labelled actual spending. */
public record BudgetView(
        boolean configured,
        String period,
        BigDecimal limitAmount,
        BigDecimal knownSubtotal,
        int unknownItemCount,
        boolean exceeded,
        BigDecimal overBy,
        boolean incomplete,
        List<CategoryTotal> categories) {
    public BudgetView {
        categories = List.copyOf(categories);
    }

    public record CategoryTotal(String category, BigDecimal knownSubtotal, int itemCount, int unknownItemCount) {
    }
}
