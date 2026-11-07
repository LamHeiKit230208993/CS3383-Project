package com.pricewatch.shopping;

import com.pricewatch.alerts.persistence.BudgetSettingsEntity.Period;

import java.math.BigDecimal;
import java.util.List;

/**
 * J07: result of applying planned budget settings to the current list estimate.
 * {@code knownSubtotal} is planned cost (never actual spending); {@code incomplete}
 * means at least one line has no current price (D06). Category totals reconcile
 * to the known subtotal.
 */
public record BudgetReport(
        Period period,
        BigDecimal plannedLimit,
        BigDecimal knownSubtotal,
        int unknownItemCount,
        boolean incomplete,
        boolean overBudget,
        List<CategoryTotal> categories) {
}
