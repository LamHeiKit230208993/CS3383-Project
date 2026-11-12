package com.pricewatch.alerts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** What the Alerts page shows. */
public record AlertPageView(
        boolean quietHours,
        List<RuleView> rules,
        List<EventView> events,
        SummaryView summary) {
    public AlertPageView {
        rules = List.copyOf(rules);
        events = List.copyOf(events);
    }

    public record RuleView(Long id, String productCode, String productName, String retailerCode,
                           String type, BigDecimal targetAmount, boolean enabled) {
    }

    public record EventView(Long id, String productCode, String productName, String retailerCode,
                            String ruleType, BigDecimal price, Instant firedAt, boolean acknowledged,
                            Instant snoozedUntil, boolean visible) {
    }

    public record SummaryView(Instant since, List<EventView> events) {
        public SummaryView {
            events = List.copyOf(events);
        }
    }
}
