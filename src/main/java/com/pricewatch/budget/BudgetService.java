package com.pricewatch.budget;

import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {
    private static final long SETTINGS_ID = 1L;
    private static final int LIMIT_PRECISION = 19;
    private static final int LIMIT_SCALE = 4;
    private static final String UNCATEGORISED = "Uncategorised";

    private final BudgetSettingsRepository settingsRepository;
    private final ShoppingService shopping;

    public BudgetService(BudgetSettingsRepository settingsRepository, ShoppingService shopping) {
        this.settingsRepository = settingsRepository;
        this.shopping = shopping;
    }

    @Transactional(readOnly = true)
    public BudgetView view() {
        var items = shopping.items();
        var summary = Summary.from(items);
        var settings = settingsRepository.findById(SETTINGS_ID).orElse(null);
        BudgetPeriod period = settings == null ? BudgetPeriod.WEEKLY : settings.getPeriod();
        BigDecimal limit = settings == null ? null : settings.getLimitAmount();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (ItemView item : items) {
            if (item.lineTotal() != null) {
                totals.merge(category(item), item.lineTotal(), BigDecimal::add);
            }
        }

        boolean overLimit = limit != null && summary.knownSubtotal().compareTo(limit) > 0;
        BudgetState state = limit == null ? BudgetState.NOT_SET
                : overLimit ? BudgetState.OVER_LIMIT
                : summary.unknownItemCount() > 0 ? BudgetState.UNKNOWN_TOTAL
                : BudgetState.WITHIN_LIMIT;
        List<CategoryTotal> categoryTotals = new ArrayList<>(totals.size());
        totals.forEach((name, amount) -> categoryTotals.add(new CategoryTotal(name, amount)));
        return new BudgetView(period, limit, summary.knownSubtotal(), summary.unknownItemCount(), categoryTotals, state);
    }

    @Transactional
    public BudgetPeriod save(String periodInput, String amountInput) {
        BudgetPeriod period = BudgetPeriod.fromInput(periodInput);
        BigDecimal amount = parseAmount(amountInput);
        settingsRepository.save(new BudgetSettingsEntity(period, amount));
        return period;
    }

    private BigDecimal parseAmount(String input) {
        final BigDecimal amount;
        try {
            amount = new BigDecimal(input == null ? "" : input.trim())
                    .setScale(LIMIT_SCALE, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw invalidAmount();
        }
        if (amount.signum() < 0 || amount.precision() > LIMIT_PRECISION) {
            throw invalidAmount();
        }
        return amount;
    }

    private IllegalArgumentException invalidAmount() {
        return new IllegalArgumentException(
                "Enter a nonnegative limit with up to 15 digits before and 4 digits after the decimal point.");
    }

    private String category(ItemView item) {
        if (item.product().category2() == null || item.product().category2().en() == null
                || item.product().category2().en().isBlank()) {
            return UNCATEGORISED;
        }
        return item.product().category2().en().trim();
    }
}
