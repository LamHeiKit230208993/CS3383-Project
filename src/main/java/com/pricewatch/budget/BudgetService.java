package com.pricewatch.budget;

import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {
    private static final String UNCATEGORISED = "Uncategorised";

    private final BudgetSettingsRepository repository;
    private final ShoppingService shopping;
    private final TransactionTemplate transactionTemplate;
    private final Object mutationLock = new Object();

    public BudgetService(BudgetSettingsRepository repository, ShoppingService shopping,
                         PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.shopping = shopping;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public BudgetSettingsEntity settings() {
        return repository.findById(1L).orElse(null);
    }

    public void setBudget(BudgetSettingsEntity.Period period, BigDecimal limitAmount) {
        if (limitAmount == null || limitAmount.signum() <= 0) {
            throw new IllegalArgumentException("Enter a planned budget greater than zero.");
        }
        if (limitAmount.scale() > 4 && limitAmount.stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException("Budget amounts support up to four decimal places.");
        }
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> {
                BudgetSettingsEntity entity = repository.findById(1L)
                        .orElseGet(() -> new BudgetSettingsEntity(period, limitAmount));
                entity.setPeriod(period);
                entity.setLimitAmount(limitAmount);
                repository.save(entity);
            });
        }
    }

    public void clearBudget() {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> repository.deleteById(1L));
        }
    }

    /**
     * Build the planned-cost view from the current My-list items. Category totals
     * reconcile with the known subtotal because they are derived from the same rows.
     */
    @Transactional(readOnly = true)
    public BudgetView view() {
        List<ItemView> items = shopping.items();
        BigDecimal knownSubtotal = BigDecimal.ZERO.setScale(4);
        int unknown = 0;
        Map<String, CategoryAccumulator> categories = new LinkedHashMap<>();
        for (ItemView item : items) {
            String category = categoryOf(item);
            CategoryAccumulator accumulator = categories.computeIfAbsent(category, CategoryAccumulator::new);
            accumulator.itemCount++;
            if (item.lineTotal() == null) {
                unknown++;
                accumulator.unknownItemCount++;
            } else {
                knownSubtotal = knownSubtotal.add(item.lineTotal());
                accumulator.knownSubtotal = accumulator.knownSubtotal.add(item.lineTotal());
            }
        }

        BudgetSettingsEntity settings = settings();
        boolean configured = settings != null;
        BigDecimal limit = configured ? settings.getLimitAmount() : null;
        String period = configured ? settings.getPeriod().name() : null;
        boolean exceeded = configured && knownSubtotal.compareTo(limit) > 0;
        BigDecimal overBy = exceeded ? knownSubtotal.subtract(limit) : null;

        List<BudgetView.CategoryTotal> categoryTotals = new ArrayList<>();
        for (CategoryAccumulator accumulator : categories.values()) {
            categoryTotals.add(new BudgetView.CategoryTotal(accumulator.category,
                    accumulator.knownSubtotal, accumulator.itemCount, accumulator.unknownItemCount));
        }
        return new BudgetView(configured, period, limit, knownSubtotal, unknown, exceeded, overBy,
                unknown > 0, categoryTotals);
    }

    private static String categoryOf(ItemView item) {
        String category2 = item.product().category2() == null ? null : item.product().category2().en();
        if (category2 != null && !category2.isBlank()) {
            return category2;
        }
        String category1 = item.product().category1() == null ? null : item.product().category1().en();
        if (category1 != null && !category1.isBlank()) {
            return category1;
        }
        return UNCATEGORISED;
    }

    private static final class CategoryAccumulator {
        private final String category;
        private BigDecimal knownSubtotal = BigDecimal.ZERO.setScale(4);
        private int itemCount;
        private int unknownItemCount;

        private CategoryAccumulator(String category) {
            this.category = category;
        }
    }
}
