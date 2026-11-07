package com.pricewatch.shopping;

import com.pricewatch.alerts.persistence.BudgetSettingsEntity;
import com.pricewatch.alerts.persistence.BudgetSettingsEntity.Period;
import com.pricewatch.alerts.repository.BudgetSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * J07: planned budget and category breakdown over the existing J04 list
 * estimate. Reuses {@link Summary} and the per-item lowest-price logic in
 * {@link ShoppingService}; unknown prices keep the estimate incomplete (D06).
 */
@Service
public class BudgetService {
    private static final String FALLBACK_CATEGORY = "Other";

    private final ShoppingService shopping;
    private final BudgetSettingsRepository settingsRepository;
    private final Clock clock;

    public BudgetService(ShoppingService shopping, BudgetSettingsRepository settingsRepository, Clock clock) {
        this.shopping = shopping;
        this.settingsRepository = settingsRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BudgetSettingsEntity settings() {
        return settingsRepository.findById(BudgetSettingsEntity.SINGLETON_ID).orElse(null);
    }

    @Transactional
    public void save(Period period, BigDecimal plannedLimit) {
        if (plannedLimit == null || plannedLimit.signum() < 0) {
            throw new IllegalArgumentException("Enter a budget of zero or more.");
        }
        BudgetSettingsEntity existing = settings();
        if (existing == null) {
            settingsRepository.save(new BudgetSettingsEntity(period, plannedLimit, clock.instant()));
        } else {
            existing.update(period, plannedLimit, clock.instant());
        }
    }

    @Transactional(readOnly = true)
    public BudgetReport report() {
        List<ItemView> items = shopping.items();
        Summary summary = Summary.from(items);

        Map<String, BigDecimal> categoryKnown = new LinkedHashMap<>();
        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        for (ItemView item : items) {
            String category = categoryOf(item);
            categoryCount.merge(category, 1, Integer::sum);
            if (item.lineTotal() != null) {
                categoryKnown.merge(category, item.lineTotal(), BigDecimal::add);
            }
        }
        List<CategoryTotal> categories = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            categories.add(new CategoryTotal(entry.getKey(),
                    categoryKnown.getOrDefault(entry.getKey(), BigDecimal.ZERO.setScale(4)),
                    entry.getValue()));
        }

        BudgetSettingsEntity settings = settings();
        Period period = settings == null ? Period.WEEKLY : settings.getPeriod();
        BigDecimal plannedLimit = settings == null ? null : settings.getPlannedLimit();
        boolean incomplete = summary.unknownItemCount() > 0;
        boolean overBudget = plannedLimit != null && summary.knownSubtotal().compareTo(plannedLimit) > 0;
        return new BudgetReport(period, plannedLimit, summary.knownSubtotal(),
                summary.unknownItemCount(), incomplete, overBudget, categories);
    }

    private static String categoryOf(ItemView item) {
        if (item.product() == null || item.product().category2() == null) {
            return FALLBACK_CATEGORY;
        }
        String en = item.product().category2().en();
        return en == null || en.isBlank() ? FALLBACK_CATEGORY : en;
    }
}
