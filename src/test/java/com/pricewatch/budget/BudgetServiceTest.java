package com.pricewatch.budget;

import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetServiceTest {
    private final BudgetSettingsRepository settings = mock(BudgetSettingsRepository.class);
    private final ShoppingService shopping = mock(ShoppingService.class);
    private final BudgetService budgets = new BudgetService(settings, shopping);

    @BeforeEach
    void noSavedSettings() {
        when(settings.findById(1L)).thenReturn(Optional.empty());
    }

    @Test
    void usesEffectiveSelectedRetailerTotalsAndReconcilesCategories() {
        when(settings.findById(1L)).thenReturn(Optional.of(
                new BudgetSettingsEntity(BudgetPeriod.WEEKLY, new BigDecimal("30.00"))));
        when(shopping.items()).thenReturn(List.of(
                item("MILK", "Dairy", "JASONS", new BigDecimal("12.5000"), new BigDecimal("25.0000"), 2, true),
                item("MILK-AUTO", "Dairy", null, new BigDecimal("7.5000"), false),
                item("BREAD", " ", null, new BigDecimal("4.2500"), false),
                item("UNKNOWN", "Dairy", "JASONS", null, false)));

        BudgetView view = budgets.view();

        assertEquals(new BigDecimal("36.7500"), view.knownSubtotal());
        assertEquals(1, view.unknownItemCount());
        assertEquals(BudgetState.OVER_LIMIT, view.state());
        assertEquals(List.of(new CategoryTotal("Dairy", new BigDecimal("32.5000")),
                        new CategoryTotal("Uncategorised", new BigDecimal("4.2500"))), view.categoryTotals());
        assertEquals(view.knownSubtotal(), view.categoryTotals().stream()
                .map(CategoryTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        verify(shopping).items();
    }

    @Test
    void marksWithinLimitUnknownWhenKnownSubtotalFitsButPriceIsMissing() {
        when(settings.findById(1L)).thenReturn(Optional.of(
                new BudgetSettingsEntity(BudgetPeriod.MONTHLY, new BigDecimal("100.0000"))));
        when(shopping.items()).thenReturn(List.of(
                item("KNOWN", "Food", null, new BigDecimal("7.5000"), false),
                item("UNKNOWN", "Food", "JASONS", null, false)));

        BudgetView view = budgets.view();

        assertEquals(BudgetState.UNKNOWN_TOTAL, view.state());
        assertEquals(1, view.unknownItemCount());
        assertEquals(new BigDecimal("7.5000"), view.knownSubtotal());
    }

    @Test
    void exactLimitIsWithinBudget() {
        when(settings.findById(1L)).thenReturn(Optional.of(
                new BudgetSettingsEntity(BudgetPeriod.WEEKLY, new BigDecimal("7.5000"))));
        when(shopping.items()).thenReturn(List.of(item("MILK", "Dairy", null,
                new BigDecimal("7.5000"), false)));

        assertEquals(BudgetState.WITHIN_LIMIT, budgets.view().state());
    }

    @Test
    void reportsNoLimitUntilTheUserSavesOne() {
        when(shopping.items()).thenReturn(List.of());

        BudgetView view = budgets.view();

        assertEquals(BudgetPeriod.WEEKLY, view.period());
        assertNull(view.limitAmount());
        assertEquals(BudgetState.NOT_SET, view.state());
    }

    @Test
    void savesWeeklyAndMonthlyLimitsAtFourDecimalPlaces() {
        when(settings.save(any(BudgetSettingsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(BudgetPeriod.WEEKLY, budgets.save("weekly", "12.5"));
        assertEquals(BudgetPeriod.MONTHLY, budgets.save("MONTHLY", "999999999999999.9999"));

        ArgumentCaptor<BudgetSettingsEntity> saved = ArgumentCaptor.forClass(BudgetSettingsEntity.class);
        verify(settings, times(2)).save(saved.capture());
        assertEquals(BudgetPeriod.WEEKLY, saved.getAllValues().getFirst().getPeriod());
        assertEquals(new BigDecimal("12.5000"), saved.getAllValues().getFirst().getLimitAmount());
        assertEquals(new BigDecimal("999999999999999.9999"), saved.getAllValues().get(1).getLimitAmount());
    }

    @Test
    void invalidPeriodAndAmountsNeverWriteSettings() {
        assertThrows(IllegalArgumentException.class, () -> budgets.save("yearly", "10"));
        assertThrows(IllegalArgumentException.class, () -> budgets.save("WEEKLY", ""));
        assertThrows(IllegalArgumentException.class, () -> budgets.save("WEEKLY", "-0.01"));
        assertThrows(IllegalArgumentException.class, () -> budgets.save("WEEKLY", "1.00001"));
        assertThrows(IllegalArgumentException.class, () -> budgets.save("WEEKLY", "1000000000000000"));
        verify(settings, never()).save(any(BudgetSettingsEntity.class));
    }

    private ItemView item(String code, String category, String selectedRetailer, BigDecimal lineTotal, boolean checked) {
        return item(code, category, selectedRetailer, lineTotal, lineTotal, 1, checked);
    }

    private ItemView item(String code, String category, String selectedRetailer, BigDecimal unitPrice,
                          BigDecimal lineTotal, int quantity, boolean checked) {
        List<ListingView> listings = code.equals("MILK")
                ? List.of(new ListingView("AEON", new BigDecimal("7.5000"), List.of()),
                        new ListingView("JASONS", new BigDecimal("12.5000"), List.of()))
                : List.of();
        ProductView product = new ProductView(code, text("Brand"), text(code), text("Root"), text(category), text(""),
                true, listings, new BigDecimal("7.5000"), listings.isEmpty() ? List.of() : List.of(listings.getFirst()));
        String retailer = selectedRetailer == null ? "AEON" : selectedRetailer;
        return new ItemView(product, quantity, checked, unitPrice, retailer, lineTotal, selectedRetailer);
    }

    private LocalizedText text(String value) {
        return new LocalizedText(value, value, value);
    }
}
