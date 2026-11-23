package com.pricewatch.notifications;

import java.math.BigDecimal;

public enum WatchPriceKind {
    TARGET, DROP_AMOUNT, DROP_PERCENT;

    public BigDecimal validate(BigDecimal value) {
        BigDecimal threshold = ProductTargetService.normalizePrice(value);
        if (this != TARGET && (threshold.signum() == 0
                || (this == DROP_PERCENT && threshold.compareTo(BigDecimal.valueOf(100)) > 0))) {
            throw new IllegalArgumentException("Enter a positive drop threshold. Percent must be at most 100.");
        }
        return threshold;
    }

    public boolean matches(BigDecimal threshold, BigDecimal current, BigDecimal previous) {
        if (this == TARGET) return current.compareTo(threshold) <= 0;
        if (previous == null) return false;
        BigDecimal drop = previous.subtract(current);
        if (this == DROP_AMOUNT) return drop.compareTo(threshold) >= 0;
        return previous.signum() > 0 && drop.multiply(BigDecimal.valueOf(100))
                .compareTo(previous.multiply(threshold)) >= 0;
    }

    public static WatchPriceKind parse(String value) {
        return value == null || value.isBlank() ? TARGET : valueOf(value);
    }
}
