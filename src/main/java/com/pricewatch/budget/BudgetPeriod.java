package com.pricewatch.budget;

import com.pricewatch.i18n.AppMessages;

public enum BudgetPeriod {
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

    BudgetPeriod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static BudgetPeriod fromInput(String value) {
        if (value != null) {
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Report invalid form input with the same message as an empty value.
            }
        }
        throw new IllegalArgumentException(AppMessages.text("error.budget.period"));
    }
}
