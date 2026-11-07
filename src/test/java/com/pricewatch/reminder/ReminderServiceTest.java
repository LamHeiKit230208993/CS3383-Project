package com.pricewatch.reminder;

import com.pricewatch.MutableClockConfig;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.alerts.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MutableClockConfig.class)
class ReminderServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private ReminderService reminders;
    @Autowired private ReminderRepository repository;
    @Autowired private MutableClockConfig.MutableClock clock;
    @Autowired private AlertEventRepository events;
    @Autowired private AlertRuleRepository rules;
    @Autowired private BudgetSettingsRepository budgetSettings;
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
        repository.deleteAllInBatch();
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
        catalog.importSample();
    }

    @Test
    void reminderRunsOnlyWhileDueAndPersistsProductAndQuantity() {
        reminders.create("P000000124", 3, 7);
        assertTrue(reminders.dueReminders().isEmpty(), "not due yet");
        var reminder = repository.findByProductCode("P000000124").orElseThrow();
        assertEquals(3, reminder.getQuantity());
        assertEquals(7, reminder.getIntervalDays());

        clock.advance(Duration.ofDays(7));
        assertEquals(1, reminders.dueReminders().size());
    }

    @Test
    void addToListUsesShoppingRulesThenReschedules() {
        reminders.create("P000000124", 2, 7);
        clock.advance(Duration.ofDays(7));
        reminders.addToList("P000000124");
        assertEquals(2, shopping.items().getFirst().quantity());
        var reminder = repository.findByProductCode("P000000124").orElseThrow();
        assertTrue(reminder.getDueAt().isAfter(clock.instant()), "rescheduled after add");
    }

    @Test
    void addToListReusesShoppingQuantityLimit() {
        reminders.create("P000000124", 999, 7);
        clock.advance(Duration.ofDays(7));
        reminders.addToList("P000000124");
        assertEquals(999, shopping.items().getFirst().quantity());
        // A second add would exceed 999, and the same conflict/limit error comes from ShoppingService.
        assertThrows(IllegalArgumentException.class, () -> shopping.add("P000000124", 1));
    }

    @Test
    void addToListRespectsRetailerConflictFromShoppingService() {
        reminders.create("P000000124", 1, 7);
        shopping.add("P000000124", 1, "JASONS");
        clock.advance(Duration.ofDays(7));
        assertThrows(IllegalArgumentException.class, () -> reminders.addToList("P000000124"),
                "reminder add goes through ShoppingService, so the retailer conflict applies");
    }

    @Test
    void clearAndSnoozeSetNextDue() {
        reminders.create("P000000124", 1, 7);
        clock.advance(Duration.ofDays(7));
        reminders.snooze("P000000124");
        var reminder = repository.findByProductCode("P000000124").orElseThrow();
        assertTrue(reminder.getDueAt().isAfter(clock.instant().plus(Duration.ofHours(23))));

        reminders.clear("P000000124");
        assertTrue(repository.findByProductCode("P000000124").isEmpty());
    }

    @Test
    void intervalBelowOneDayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> reminders.create("P000000124", 1, 0));
    }
}
