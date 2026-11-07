package com.pricewatch;

import com.pricewatch.alerts.AlertService;
import com.pricewatch.alerts.persistence.AlertRuleEntity.Type;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.alerts.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.BudgetService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The issue's combined flow: a tracked price update creates the expected alert,
 * updates the list estimate/budget result, and a reminder adds the intended item.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MutableClockConfig.class)
class CombinedFlowTest {
    @Autowired private CatalogService catalog;
    @Autowired private TrackingService tracking;
    @Autowired private AlertService alerts;
    @Autowired private BudgetService budget;
    @Autowired private ReminderService reminders;
    @Autowired private ShoppingService shopping;
    @Autowired private MutableClockConfig.MutableClock clock;
    @Autowired private AlertEventRepository events;
    @Autowired private AlertRuleRepository rules;
    @Autowired private BudgetSettingsRepository budgetSettings;
    @Autowired private ReminderRepository remindersRepository;
    @Autowired private PriceObservationRepository observations;
    @Autowired private TrackingChoiceRepository choices;
    @Autowired private WishlistEntryRepository wishlist;
    @Autowired private ProductRepository productRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;

    @BeforeEach
    void clear() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        remindersRepository.deleteAllInBatch();
        events.deleteAllInBatch();
        rules.deleteAllInBatch();
        budgetSettings.deleteAllInBatch();
        observations.deleteAllInBatch();
        choices.deleteAllInBatch();
        wishlist.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        clock.set(java.time.Instant.parse("2026-10-31T08:00:00Z"));
    }

    @Test
    void trackedPriceUpdateAlertsBudgetAndReminderTogether() {
        accept(product("9.00"));
        tracking.save("P000000124");
        assertFalse(tracking.isTracked("P000000124"), "saving alone does not track");

        tracking.enableTracking("P000000124");
        alerts.createRule("P000000124", Type.DROP, null);
        budget.save(com.pricewatch.alerts.persistence.BudgetSettingsEntity.Period.WEEKLY, new BigDecimal("50"));
        shopping.add("P000000124", 2);

        // Re-import 9.00 so the tracked period has a known baseline observation.
        accept(product("9.00"));
        BigDecimal firstBudget = budget.report().knownSubtotal();
        assertEquals(0, events.count());

        accept(product("7.50"));
        assertEquals(1, events.count(), "tracked price drop creates an alert");
        assertTrue(budget.report().knownSubtotal().compareTo(firstBudget) < 0,
                "list estimate falls with the new price");

        reminders.create("P000000124", 1, 7);
        clock.advance(Duration.ofDays(7));
        reminders.addToList("P000000124");
        assertEquals(3, shopping.items().getFirst().quantity(), "reminder adds its quantity to My list");
    }

    private void accept(String... records) {
        catalog.accept(("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8), "SAMPLE", "test");
    }

    private String product(String price) {
        return """
                {"code":"P000000124","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk / Milk drinks","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":[{"supermarketCode":"AEON","price":"%s"}],"offers":[]}
                """.formatted(price).trim();
    }
}
