package com.pricewatch.reminders;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.reminders.repository.ReminderRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReminderServiceTest {
    static final ZoneId HK = ZoneId.of("Asia/Hong_Kong");
    static final MutableClock CLOCK = new MutableClock(Instant.parse("2026-11-10T04:00:00Z"));

    @TestConfiguration
    static class ClockOverride {
        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }

    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private ReminderService reminders;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;
    @Autowired private PriceObservationRepository observationRepository;
    @Autowired private TrackingChoiceRepository trackingRepository;
    @Autowired private WishlistEntryRepository wishlistRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void clearDatabase() {
        reminderRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        wishlistRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        CLOCK.set(Instant.parse("2026-11-10T04:00:00Z"));
    }

    @Test
    void reminderDueStateComputedFromClockAndSurvivesActions() {
        catalog.importSample();
        reminders.create("P000000124", 2, 10, 1);

        ReminderView initial = reminders.reminders().getFirst();
        assertFalse(initial.due());
        assertEquals(2, initial.quantity());

        CLOCK.set(CLOCK.instant().plus(11, ChronoUnit.DAYS));
        assertTrue(reminders.reminders().getFirst().due());

        // Adding to the list does not change reminder state (D07).
        reminders.addToList(initial.id());
        assertEquals(2, shopping.items().getFirst().quantity());
        assertTrue(reminders.reminders().getFirst().due());
    }

    @Test
    void clearDeactivatesReminder() {
        catalog.importSample();
        reminders.create("P000000124", 1, 5, 1);
        Long id = reminders.reminders().getFirst().id();

        reminders.clear(id);

        ReminderView cleared = reminders.reminders().getFirst();
        assertFalse(cleared.active());
        assertFalse(cleared.due());
    }

    @Test
    void snoozePushesNextDueBySnoozeDays() {
        catalog.importSample();
        reminders.create("P000000124", 1, 5, 2);
        Long id = reminders.reminders().getFirst().id();
        Instant due = reminders.reminders().getFirst().nextDueAt();

        reminders.snooze(id);

        assertEquals(due.plus(2, ChronoUnit.DAYS), reminders.reminders().getFirst().nextDueAt());
    }

    @Test
    void addToListReusesListConflictRule() {
        catalog.importSample();
        shopping.add("P000000124", 1, "JASONS");
        reminders.create("P000000124", 1, 5, 1);
        Long id = reminders.reminders().getFirst().id();

        // Automatic add conflicts with the existing explicit JASONS choice.
        assertThrows(IllegalArgumentException.class, () -> reminders.addToList(id));
    }

    @Test
    void rejectsInvalidIntervalsAndQuantities() {
        catalog.importSample();
        assertThrows(IllegalArgumentException.class, () -> reminders.create("P000000124", 0, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> reminders.create("P000000124", 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> reminders.create("P000000124", 1, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> reminders.create("MISSING", 1, 5, 1));
    }

    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override public ZoneId getZone() { return HK; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
