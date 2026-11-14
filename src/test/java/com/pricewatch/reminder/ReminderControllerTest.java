package com.pricewatch.reminder;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.NotificationPolicy;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Autowired private MockMvc mvc;
    @Autowired private CatalogService catalog;
    @Autowired private ReminderService reminders;
    @Autowired private ReminderRepository reminderRepository;
    @Autowired private ShoppingService shopping;
    @Autowired private ShoppingListItemRepository listItems;
    @Autowired private ShoppingListRepository lists;
    @Autowired private AcceptedImportRepository imports;
    @Autowired private OfferRepository offers;
    @Autowired private RetailerListingRepository listings;
    @Autowired private ProductRepository products;

    @MockitoBean private Clock clock;
    @MockitoBean private NotificationPolicy notificationPolicy;

    private AtomicReference<Instant> now;

    @BeforeEach
    void resetDataAndClock() {
        reminderRepository.deleteAllInBatch();
        listItems.deleteAllInBatch();
        lists.deleteAllInBatch();
        imports.deleteAllInBatch();
        offers.deleteAllInBatch();
        listings.deleteAllInBatch();
        products.deleteAllInBatch();
        now = new AtomicReference<>(START);
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(notificationPolicy.isQuiet(any())).thenReturn(false);
        when(notificationPolicy.nextAllowed(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPersistsProductQuantityIntervalRetailerAndInitialDueTime() throws Exception {
        catalog.importSample();

        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "3")
                        .param("intervalDays", "7").param("retailerCode", "JASONS"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/reminders"));

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
    void candidatePageShowsAccessibleIntervalAndOptionalRetailerChoice() throws Exception {
        catalog.importSample();

        mvc.perform(get("/reminders").param("code", "P000000124"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("Packs to remind me about")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("name=\"intervalDays\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("JASONS")));
    }

    @Test
    void snoozeMovesCurrentDueByTwentyFourHoursAndClearStartsIntervalFromActionTime() throws Exception {
        catalog.importSample();
        create("2", "7", "");
        ReminderView before = reminders.overview().reminders().getFirst();

        now.set(before.nextDueAt());
        mvc.perform(post("/reminders/{id}/snooze", before.id())).andExpect(redirectedUrl("/reminders"));
        ReminderView snoozed = reminders.overview().reminders().getFirst();
        assertEquals(before.nextDueAt().plusSeconds(24 * 60 * 60L), snoozed.effectiveDueAt());
        assertEquals(before.nextDueAt(), snoozed.nextDueAt());

        now.set(snoozed.effectiveDueAt());
        mvc.perform(post("/reminders/{id}/clear", before.id())).andExpect(redirectedUrl("/reminders"));
        ReminderView cleared = reminders.overview().reminders().getFirst();
        assertEquals(now.get().plusSeconds(7 * 24 * 60 * 60L), cleared.nextDueAt());
        assertEquals(cleared.nextDueAt(), cleared.effectiveDueAt());
        assertFalse(cleared.due());
    }

    @Test
    void quietHoursHideDueIndicatorButManualActionsRemainAvailable() throws Exception {
        catalog.importSample();
        create("1", "1", "");
        ReminderView reminder = reminders.overview().reminders().getFirst();
        now.set(reminder.nextDueAt());
        when(notificationPolicy.isQuiet(any())).thenReturn(true);
        when(notificationPolicy.nextAllowed(any())).thenReturn(reminder.nextDueAt().plusSeconds(8 * 60 * 60L));

        assertTrue(reminders.overview().quietHours());
        assertFalse(reminders.overview().reminders().getFirst().due());
        mvc.perform(get("/reminders")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("Due status is hidden during quiet hours")));
        mvc.perform(post("/reminders/{id}/clear", reminder.id())).andExpect(redirectedUrl("/reminders"));
        assertEquals(now.get().plusSeconds(24 * 60 * 60L), reminders.overview().reminders().getFirst().nextDueAt());
    }

    @Test
    void failedListAddKeepsReminderAndListUnchangedThenSuccessfulRetryDoesNotClearIt() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1, "AEON");
        create("2", "1", "JASONS");
        ReminderView reminder = reminders.overview().reminders().getFirst();
        now.set(reminder.nextDueAt());

        mvc.perform(post("/reminders/{id}/add", reminder.id()))
                .andExpect(redirectedUrl("/reminders")).andExpect(flash().attribute("error",
                        "This product is already in your list with another retailer; change retailer in My list."));
        assertEquals(1, shopping.items().getFirst().quantity());
        assertEquals("AEON", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(reminder.effectiveDueAt(), reminders.overview().reminders().getFirst().effectiveDueAt());
        assertTrue(reminders.overview().reminders().getFirst().due());

        shopping.remove("P000000124");
        mvc.perform(post("/reminders/{id}/add", reminder.id())).andExpect(redirectedUrl("/reminders"));
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
                .andExpect(redirectedUrl("/reminders")).andExpect(flash().attributeExists("error"));
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
                .andExpect(redirectedUrl("/reminders?code=P000000124")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "1000")
                        .param("intervalDays", "4"))
                .andExpect(redirectedUrl("/reminders?code=P000000124")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", "999")
                        .param("intervalDays", "0"))
                .andExpect(redirectedUrl("/reminders?code=P000000124")).andExpect(flash().attributeExists("error"));
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
                .andExpect(redirectedUrl("/reminders"))
                .andExpect(flash().attribute("error", "Quantity must be a whole number from 1 to 999."));

        assertEquals(1, shopping.items().getFirst().quantity());
        assertEquals(1, reminderRepository.count());
        assertEquals(reminder.effectiveDueAt(), reminders.overview().reminders().getFirst().effectiveDueAt());
    }

    private void create(String quantity, String intervalDays, String retailerCode) throws Exception {
        mvc.perform(post("/reminders/create").param("code", "P000000124").param("quantity", quantity)
                        .param("intervalDays", intervalDays).param("retailerCode", retailerCode))
                .andExpect(redirectedUrl("/reminders"));
    }
}
