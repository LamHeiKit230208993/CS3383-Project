package com.pricewatch.reminder;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.NotificationScheduler;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.reminder.repository.ReminderOccurrenceRepository;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reminder-controller-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"
})
@AutoConfigureMockMvc
class ReminderControllerTest {
    private static final Instant START = Instant.parse("2026-11-06T02:00:00Z");
    private static final String RULES = "/alerts/rules?code=P000000124";

    @Autowired private MockMvc mvc;
    @Autowired private CatalogService catalog;
    @Autowired private ReminderService reminders;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private ReminderOccurrenceRepository occurrenceRepository;
    @Autowired private ShoppingService shopping;
    @Autowired private ShoppingListItemRepository listItems;
    @Autowired private ShoppingListRepository lists;
    @Autowired private AcceptedImportRepository imports;
    @Autowired private OfferRepository offers;
    @Autowired private RetailerListingRepository listings;
    @Autowired private ProductRepository products;
    @Autowired private WatchRuleService watchRules;
    @Autowired private ProductTargetService alerts;

    @MockitoBean private Clock clock;

    private AtomicReference<Instant> now;

    @BeforeEach
    void resetDataAndClock() {
        occurrenceRepository.deleteAllInBatch();
        reminderRepository.deleteAllInBatch();
        listItems.deleteAllInBatch();
        lists.deleteAllInBatch();
        imports.deleteAllInBatch();
        offers.deleteAllInBatch();
        listings.deleteAllInBatch();
        products.deleteAllInBatch();
        now = new AtomicReference<>(START);
        when(clock.instant()).thenAnswer(invocation -> now.get());
    }

    @Test
    void createPersistsProductQuantityIntervalRetailerAndInitialDueTime() throws Exception {
        catalog.importSample();

        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "3")
                        .param("intervalDays", "7").param("retailerCode", "JASONS"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(RULES));

        ReminderView saved = reminders.overview().reminders().getFirst();
        assertEquals("P000000124", saved.productCode());
        assertEquals(3, saved.quantity());
        assertEquals(7, saved.intervalDays());
        assertEquals("JASONS", saved.selectedRetailerCode());
        assertEquals(START.plusSeconds(7 * 24 * 60 * 60L), saved.nextDueAt());
        assertTrue(saved.currentPriceAvailable());
        assertFalse(saved.due());
    }

    @Test
    void legacyReminderLinksOpenUnifiedAlertsOrTheItemRulePanel() throws Exception {
        mvc.perform(get("/reminders")).andExpect(redirectedUrl("/alerts"));
        mvc.perform(get("/reminders").param("code", "P000000124"))
                .andExpect(redirectedUrl(RULES));
    }

    @Test
    void pausingKeepsTheScheduleAndHistoryAndBlocksActionsUntilResumed() throws Exception {
        catalog.importSample();
        create("2", "7", "JASONS");
        var rule = reminders.overview().reminders().getFirst();
        assertTrue(rule.enabled());

        mvc.perform(post("/reminders/{id}/enabled", rule.id()).param("enabled", "false").param("from", "center"))
                .andExpect(redirectedUrl("/alerts?tab=rules")).andExpect(flash().attributeExists("notice"));
        now.set(rule.nextDueAt());
        reminders.recordDue();
        assertTrue(reminders.occurrences(now.get()).isEmpty());
        var paused = reminders.overview().reminders().getFirst();
        assertFalse(paused.enabled());
        assertFalse(paused.due());
        for (String action : java.util.List.of("clear", "snooze", "add")) {
            mvc.perform(post("/reminders/{id}/" + action, rule.id()))
                    .andExpect(redirectedUrl("/alerts")).andExpect(flash().attributeExists("error"));
        }
        assertEquals(rule.nextDueAt(), reminders.overview().reminders().getFirst().nextDueAt());
        assertTrue(shopping.items().isEmpty());

        mvc.perform(post("/reminders/{id}/enabled", rule.id()).param("enabled", "true")
                        .param("code", rule.productCode()).param("from", "saved"))
                .andExpect(redirectedUrl(RULES + "&from=saved")).andExpect(flash().attributeExists("notice"));
        reminders.recordDue();
        var due = reminders.occurrences(now.get()).getFirst();
        assertEquals(rule.nextDueAt(), due.dueAt());
        assertTrue(due.active());

        reminders.setEnabled(rule.id(), false);
        for (String action : java.util.List.of("clear", "snooze", "add")) {
            mvc.perform(post("/alerts/reminders/{id}/" + action, due.id()))
                    .andExpect(redirectedUrl("/alerts")).andExpect(flash().attributeExists("error"));
        }
        var historical = reminders.occurrences(now.get()).getFirst();
        assertEquals(due.id(), historical.id());
        assertFalse(historical.active());
        assertFalse(historical.read());

        mvc.perform(post("/reminders/{id}/enabled", rule.id()).param("enabled", "true")
                        .param("code", rule.productCode()).param("from", "rules"))
                .andExpect(redirectedUrl(RULES + "&from=rules"));
        reminders.recordDue();
        assertEquals(java.util.List.of(due), reminders.occurrences(now.get()));
        mvc.perform(post("/reminders/{id}/delete", rule.id()).param("code", rule.productCode()).param("from", "center"))
                .andExpect(redirectedUrl("/alerts?tab=rules"));
        assertTrue(reminders.overview().reminders().isEmpty());
        assertEquals(due.id(), reminders.occurrences(now.get()).getFirst().id());
    }

    @Test
    void snoozeMovesCurrentDueByTwentyFourHoursAndClearStartsIntervalFromActionTime() throws Exception {
        catalog.importSample();
        create("2", "7", "");
        ReminderView before = reminders.overview().reminders().getFirst();

        now.set(before.nextDueAt());
        reminders.recordDue();
        var occurrence = reminders.occurrences(now.get()).getFirst();
        mvc.perform(post("/alerts/reminders/{id}/snooze", occurrence.id())).andExpect(redirectedUrl("/alerts"));
        ReminderView snoozed = reminders.overview().reminders().getFirst();
        assertEquals(before.nextDueAt().plusSeconds(24 * 60 * 60L), snoozed.effectiveDueAt());
        assertEquals(before.nextDueAt(), snoozed.nextDueAt());
        assertFalse(reminders.occurrences(now.get()).getFirst().available());

        now.set(snoozed.effectiveDueAt());
        reminders.recordDue();
        assertEquals(occurrence.id(), reminders.occurrences(now.get()).getFirst().id());
        assertEquals(1, occurrenceRepository.count());
        mvc.perform(post("/alerts/reminders/{id}/clear", occurrence.id())).andExpect(redirectedUrl("/alerts"));
        ReminderView cleared = reminders.overview().reminders().getFirst();
        assertEquals(now.get().plusSeconds(7 * 24 * 60 * 60L), cleared.nextDueAt());
        assertEquals(cleared.nextDueAt(), cleared.effectiveDueAt());
        assertFalse(cleared.due());
        var dismissed = reminders.occurrences(now.get()).getFirst();
        assertTrue(dismissed.read());
        assertFalse(dismissed.active());
        assertTrue(dismissed.available());

        now.set(cleared.nextDueAt());
        reminders.recordDue();
        var events = reminders.occurrences(now.get());
        assertEquals(2, events.size());
        assertTrue(events.getFirst().active());
        assertEquals(occurrence.id(), events.getLast().id());
        mvc.perform(post("/alerts/reminders/{id}/clear", occurrence.id()))
                .andExpect(redirectedUrl("/alerts")).andExpect(flash().attributeExists("error"));
        assertEquals(cleared.nextDueAt(), reminders.overview().reminders().getFirst().nextDueAt());
    }

    @Test
    void canClearASnoozedOccurrenceBeforeItsEffectiveDueTime() throws Exception {
        catalog.importSample();
        create("1", "1", "");
        var rule = reminders.overview().reminders().getFirst();
        now.set(rule.nextDueAt());
        reminders.recordDue();
        long id = reminders.occurrences(now.get()).getFirst().id();
        reminders.snoozeOccurrence(id);

        mvc.perform(post("/alerts/reminders/{id}/clear", id))
                .andExpect(redirectedUrl("/alerts")).andExpect(flash().attributeExists("notice"));
        assertTrue(reminders.occurrences(now.get()).getFirst().read());
        assertEquals(now.get().plusSeconds(24 * 60 * 60L), reminders.overview().reminders().getFirst().nextDueAt());
    }

    @Test
    void simultaneousChecksRecordOneDueOccurrenceAndDeletingRuleKeepsItInHistory() throws Exception {
        catalog.importSample();
        create("1", "1", "JASONS");
        ReminderView reminder = reminders.overview().reminders().getFirst();
        now.set(reminder.nextDueAt());

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var checks = java.util.List.of(executor.submit(() -> recordDueTogether(ready, start)),
                    executor.submit(() -> recordDueTogether(ready, start)));
            try {
                assertTrue(ready.await(5, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            for (var check : checks) check.get(10, TimeUnit.SECONDS);
        }

        assertEquals(1, occurrenceRepository.count());
        var due = reminders.occurrences(now.get()).getFirst();
        assertTrue(due.active());
        assertTrue(due.available());
        assertEquals(reminder.nextDueAt(), due.dueAt());

        mvc.perform(post("/reminders/{id}/delete", reminder.id()).param("code", "P000000124"))
                .andExpect(redirectedUrl(RULES));
        assertEquals(0, reminderRepository.count());
        var history = reminders.occurrences(now.get()).getFirst();
        assertEquals(due.id(), history.id());
        assertEquals("P000000124", history.productCode());
        assertEquals("JASONS", history.retailerCode());
        assertTrue(history.read());
        assertFalse(history.active());
    }

    private Void recordDueTogether(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        reminders.recordDue();
        return null;
    }

    @Test
    void editingDetailsKeepsTheDueTimeAndChangingTheIntervalReschedules() throws Exception {
        catalog.importSample();
        create("2", "7", "JASONS");
        ReminderView original = reminders.overview().reminders().getFirst();
        now.set(original.nextDueAt());
        reminders.recordDue();

        mvc.perform(post("/reminders/{id}/update", original.id()).param("code", "P000000124")
                .param("quantity", "3").param("intervalDays", "7").param("retailerCode", "AEON"))
                .andExpect(redirectedUrl(RULES));
        ReminderView edited = reminders.overview().reminders().getFirst();
        assertEquals(3, edited.quantity());
        assertEquals("AEON", edited.selectedRetailerCode());
        assertEquals(original.nextDueAt(), edited.nextDueAt());
        var occurrence = reminders.occurrences(now.get()).getFirst();
        assertEquals(3, occurrence.quantity());
        assertEquals("AEON", occurrence.retailerCode());
        mvc.perform(post("/alerts/reminders/{id}/add", occurrence.id())).andExpect(redirectedUrl("/alerts"));
        assertEquals(3, shopping.items().getFirst().quantity());
        assertEquals("AEON", shopping.items().getFirst().selectedRetailerCode());

        mvc.perform(post("/reminders/{id}/update", original.id()).param("code", "P000000124")
                .param("quantity", "3").param("intervalDays", "14").param("retailerCode", "AEON"))
                .andExpect(redirectedUrl(RULES));
        ReminderView rescheduled = reminders.overview().reminders().getFirst();
        assertEquals(14, rescheduled.intervalDays());
        assertEquals(now.get().plusSeconds(14 * 24 * 60 * 60L), rescheduled.nextDueAt());
    }

    @Test
    void failedListAddKeepsReminderAndListUnchangedThenSuccessfulRetryDoesNotClearIt() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1, "AEON");
        create("2", "1", "JASONS");
        ReminderView reminder = reminders.overview().reminders().getFirst();
        now.set(reminder.nextDueAt());

        reminders.recordDue();
        long alertId = reminders.occurrences(now.get()).getFirst().id();
        mvc.perform(post("/alerts/reminders/{id}/add", alertId))
                .andExpect(redirectedUrl("/alerts")).andExpect(flash().attribute("error",
                        "This product is already in your list with another retailer; change retailer in My list."));
        assertEquals(1, shopping.items().getFirst().quantity());
        assertEquals("AEON", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(reminder.effectiveDueAt(), reminders.overview().reminders().getFirst().effectiveDueAt());
        assertTrue(reminders.overview().reminders().getFirst().due());

        shopping.remove("P000000124");
        mvc.perform(post("/alerts/reminders/{id}/add", alertId)).andExpect(redirectedUrl("/alerts"));
        assertEquals(2, shopping.items().getFirst().quantity());
        assertEquals("JASONS", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(1, reminderRepository.count());
        assertTrue(reminders.overview().reminders().getFirst().due());
    }

    @Test
    void unavailableSelectedPriceStaysUnknownAndCannotPartiallyAdd() throws Exception {
        catalog.importSample();
        create("2", "5", "JASONS");
        ReminderView reminder = reminders.overview().reminders().getFirst();
        offers.deleteAllInBatch();
        listings.deleteAllInBatch();

        ReminderView withoutPrice = reminders.overview().reminders().getFirst();
        assertFalse(withoutPrice.currentPriceAvailable());
        mvc.perform(post("/reminders/{id}/add", reminder.id()))
                .andExpect(redirectedUrl("/alerts")).andExpect(flash().attributeExists("error"));
        assertTrue(shopping.items().isEmpty());
        ReminderView retained = reminders.overview().reminders().getFirst();
        assertEquals("JASONS", retained.selectedRetailerCode());
        assertFalse(retained.currentPriceAvailable());
        assertEquals(reminder.effectiveDueAt(), retained.effectiveDueAt());
    }

    @Test
    void quantityAndIntervalValidationRejectsInvalidValuesWithoutSaving() throws Exception {
        catalog.importSample();

        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "0")
                        .param("intervalDays", "4"))
                .andExpect(redirectedUrl(RULES)).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "1000")
                        .param("intervalDays", "4"))
                .andExpect(redirectedUrl(RULES)).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "999")
                        .param("intervalDays", "0"))
                .andExpect(redirectedUrl(RULES)).andExpect(flash().attributeExists("error"));
        assertEquals(0, reminderRepository.count());
        assertTrue(reminders.overview().reminders().isEmpty());
    }

    @Test
    void addingReminderBeyondListQuantityLimitKeepsBothRecordsUnchanged() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1);
        create("999", "3", "");
        ReminderView reminder = reminders.overview().reminders().getFirst();

        mvc.perform(post("/reminders/{id}/add", reminder.id()))
                .andExpect(redirectedUrl("/alerts"))
                .andExpect(flash().attribute("error", "Quantity must be a whole number from 1 to 999."));

        assertEquals(1, shopping.items().getFirst().quantity());
        assertEquals(1, reminderRepository.count());
        assertEquals(reminder.effectiveDueAt(), reminders.overview().reminders().getFirst().effectiveDueAt());
    }

    private void create(String quantity, String intervalDays, String retailerCode) throws Exception {
        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", quantity)
                        .param("intervalDays", intervalDays).param("retailerCode", retailerCode))
                .andExpect(redirectedUrl(RULES));
    }

    @Test
    void invalidProductRetailerAndDateLeaveNoStoredRule() {
        catalog.importSample();
        for (String invalid : new String[]{null, " ", "MISSING"}) {
            assertEquals("Choose a product from the current catalogue.", assertThrows(
                    IllegalArgumentException.class, () -> reminders.create(invalid, 1, 1, null)).getMessage());
        }
        assertEquals("Choose a listed retailer for this product, or use Automatic lowest.",
                assertThrows(IllegalArgumentException.class,
                        () -> reminders.create("P000000124", 1, 1, "OTHER")).getMessage());
        now.set(Instant.MAX);
        assertEquals("That interval is outside the supported date range.", assertThrows(
                IllegalArgumentException.class, () -> reminders.create("P000000124", 1, 1, null)).getMessage());
        assertEquals(0, reminderRepository.count());
    }

    @Test
    void snoozingBeforeDueDoesNotCreateHistoryAndBlocksOccurrenceActionsUntilItExpires() {
        catalog.importSample();
        reminders.create("P000000124", 2, 1, " AEON ");
        var rule = reminders.overview().reminders().getFirst();
        reminders.snooze(rule.id());
        assertTrue(reminders.occurrences(now.get()).isEmpty());
        now.set(reminders.overview().reminders().getFirst().effectiveDueAt());
        reminders.recordDue();
        var occurrence = reminders.occurrences(now.get()).getFirst();
        reminders.snoozeOccurrence(occurrence.id());
        assertThrows(IllegalArgumentException.class, () -> reminders.addOccurrence(occurrence.id()));
        assertThrows(IllegalArgumentException.class, () -> reminders.snoozeOccurrence(occurrence.id()));
        assertTrue(shopping.items().isEmpty());
        assertFalse(reminders.occurrences(now.get()).getFirst().available());

        now.set(reminders.overview().reminders().getFirst().effectiveDueAt());
        reminders.addOccurrence(occurrence.id());
        assertEquals(2, shopping.items().getFirst().quantity());
        assertEquals("AEON", shopping.items().getFirst().selectedRetailerCode());
    }

    @Test
    void ruleSnoozeUpdatesTheDueOccurrenceAndClearStartsTheNextInterval() {
        catalog.importSample();
        reminders.create("P000000124", 1, 1, null);
        var rule = reminders.overview().reminders().getFirst();
        now.set(rule.nextDueAt());
        reminders.snooze(rule.id());
        var snoozed = reminders.occurrences(now.get()).getFirst();
        assertFalse(snoozed.available());
        assertTrue(snoozed.active());
        reminders.clear(rule.id());
        var cleared = reminders.occurrences(now.get()).getFirst();
        assertEquals(snoozed.id(), cleared.id());
        assertTrue(cleared.read());
        assertFalse(cleared.active());
        assertEquals(now.get().plusSeconds(86_400), reminders.overview().reminders().getFirst().nextDueAt());
    }

    @Test
    void schedulerRecordsBothFamiliesOfDueRemindersAndDoesNotRepeatThem() {
        catalog.importSample();
        reminders.create("P000000124", 1, 1, null);
        Instant due = reminders.overview().reminders().getFirst().nextDueAt();
        watchRules.save("P000000229", null, null,
                LocalDateTime.ofInstant(due, ZoneId.of("Asia/Hong_Kong")), "ONCE");
        var scheduler = new NotificationScheduler(reminders, watchRules);
        try {
            scheduler.processDueEvents();
            assertTrue(reminders.occurrences(now.get()).isEmpty());
            assertTrue(alerts.events(now.get()).isEmpty());
            now.set(due);
            scheduler.processDueEvents();
            scheduler.processDueEvents();
            assertEquals(java.util.List.of("P000000124"),
                    reminders.occurrences(now.get()).stream().map(ReminderOccurrenceView::productCode).toList());
            assertEquals(java.util.List.of("P000000229"),
                    alerts.events(now.get()).stream().map(event -> event.productCode()).toList());
        } finally {
            alerts.reset();
            watchRules.reset();
        }
    }
}
