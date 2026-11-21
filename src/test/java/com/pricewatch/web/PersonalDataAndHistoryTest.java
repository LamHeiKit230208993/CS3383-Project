package com.pricewatch.web;

import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.importer.CatalogJsonParser;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * J12 R45 (view/export/delete local personal data) and R10/R14 (history shows
 * only real dated records, with honest gaps and empty states).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:j12data;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"
})
@AutoConfigureMockMvc
class PersonalDataAndHistoryTest {
    private static final Instant START = Instant.parse("2026-11-06T04:00:00Z");
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired CatalogJsonParser parser;
    @Autowired CatalogReplacementService replacement;
    @Autowired ShoppingService shopping;
    @Autowired TrackingService tracking;
    @Autowired ReminderService reminders;
    @Autowired PersonalDataService personalData;
    @Autowired HistoryService history;
    @Autowired ProductRepository productRepository;
    @Autowired RetailerListingRepository listingRepository;
    @Autowired OfferRepository offerRepository;
    @Autowired AcceptedImportRepository acceptedImportRepository;
    @Autowired ShoppingListRepository listRepository;
    @Autowired ShoppingListItemRepository listItemRepository;
    @Autowired SavedProductRepository savedRepository;
    @Autowired TrackingRepository trackingRepository;
    @Autowired PriceObservationRepository observationRepository;
    @Autowired AlertEventRepository alertEventRepository;
    @Autowired NotificationSettingsRepository notificationSettingsRepository;
    @Autowired ReminderRepository reminderRepository;
    @Autowired BudgetSettingsRepository budgetSettingsRepository;
    @MockitoBean Clock appClock;

    private final AtomicReference<Instant> now = new AtomicReference<>(START);

    @BeforeEach
    void clearDatabase() {
        now.set(START);
        when(appClock.instant()).thenAnswer(invocation -> now.get());
        alertEventRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        savedRepository.deleteAllInBatch();
        reminderRepository.deleteAllInBatch();
        listItemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        notificationSettingsRepository.deleteAllInBatch();
        budgetSettingsRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        acceptedImportRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void myDataShowsCountsAndExportsOnlyUserRecords() throws Exception {
        catalog.importTestCatalogue(json("DATA-1", "5.00"), "LIVE", "fixture-live");
        shopping.add("DATA-1", 2);
        tracking.save("DATA-1");
        tracking.start("DATA-1", "STORE-A");
        reminders.create("DATA-1", 1, 30, "STORE-A");

        mvc.perform(get("/my-data")).andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("Language")))
                .andExpect(content().string(containsString("High contrast")))
                .andExpect(content().string(containsString("Export my data")));

        byte[] export = mvc.perform(get("/my-data/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("market-desk-my-data.json")))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andReturn().getResponse().getContentAsByteArray();
        String json = new String(export, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"code\" : \"DATA-1\""), json);
        assertTrue(json.contains("\"kind\" : \"local-user-data\""), json);
        assertTrue(json.contains("\"observations\""), json);
        assertFalse(json.contains("cat2Name"), "Source catalogue records are not personal data.");
    }

    @Test
    void deleteRequiresTheWordAndThenRemovesUserRecordsButKeepsTheCatalogue() throws Exception {
        catalog.importTestCatalogue(json("DATA-2", "5.00"), "LIVE", "fixture-live");
        shopping.add("DATA-2", 1);
        tracking.save("DATA-2");
        tracking.start("DATA-2", "STORE-A");
        reminders.create("DATA-2", 1, 30, null);
        assertEquals(1, shopping.items().size());

        mvc.perform(post("/my-data/delete").param("confirm", "nope"))
                .andExpect(redirectedUrl("/my-data"));
        assertEquals(1, shopping.items().size(), "A wrong confirmation must not delete anything.");

        mvc.perform(post("/my-data/delete").param("confirm", "DELETE"))
                .andExpect(redirectedUrl("/my-data"));
        assertTrue(shopping.items().isEmpty());
        assertEquals(0, savedRepository.count());
        assertEquals(0, trackingRepository.count());
        assertEquals(0, observationRepository.count());
        assertEquals(0, reminderRepository.count());
        assertEquals(1, productRepository.count(), "The accepted catalogue is kept.");
        assertNotNull(catalog.status(), "Import metadata is kept so the catalogue is still usable.");
    }

    @Test
    void historyPageShowsAnHonestEmptyStateBeforeAnyObservation() throws Exception {
        catalog.importTestCatalogue(json("DATA-3", "5.00"), "SAMPLE", "fixture-sample");
        mvc.perform(get("/history")).andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("No priced records yet.")))
                .andExpect(content().string(not(containsString("No records"))));
    }

    @Test
    void historyPageShowsRealDatedRecordsAndMarksMissingDays() throws Exception {
        catalog.importTestCatalogue(json("DATA-4", "5.00"), "LIVE", "fixture-live");
        tracking.start("DATA-4", "STORE-A");
        now.set(START.plusSeconds(4L * 24 * 60 * 60));
        catalog.importTestCatalogue(json("DATA-4", "4.00"), "LIVE", "fixture-live");
        now.set(START.plusSeconds(4L * 24 * 60 * 60 + 60));
        catalog.importTestCatalogue(json("DATA-4", "4.50"), "LIVE", "fixture-live");

        mvc.perform(get("/history")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Price history.")))
                .andExpect(content().string(containsString("No records")))
                .andExpect(content().string(not(containsString("No priced records yet."))))
                .andExpect(content().contentTypeCompatibleWith("text/html"));

        var rows = history.products().getFirst().retailers().getFirst().rows();
        assertEquals(4, rows.size(), "3 stored records plus 1 visible gap");
        assertEquals(1, rows.stream().filter(HistoryService.Row::isGap).count());
        var gap = rows.stream().filter(HistoryService.Row::isGap).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 7), gap.gapFrom());
        assertEquals(LocalDate.of(2026, 11, 9), gap.gapTo());
    }

    @Test
    void historyPageAndMyDataAreNormalDestinations() throws Exception {
        mvc.perform(get("/my-data")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Type DELETE to confirm")));
        mvc.perform(get("/history")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Manage tracking")));
    }

    @Test
    void overviewCountsMatchTheStoredRows() {
        catalog.importTestCatalogue(json("DATA-5", "5.00"), "LIVE", "fixture-live");
        shopping.add("DATA-5", 3);
        tracking.save("DATA-5");
        tracking.start("DATA-5", "STORE-A");
        var overview = personalData.overview();
        assertEquals(1, overview.listItems());
        assertEquals(1, overview.savedProducts());
        assertEquals(1, overview.trackingPeriods());
        assertEquals(1, overview.priceObservations());
        assertEquals(0, overview.reminders());
    }

    private byte[] json(String code, String price) {
        String product = """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"產品","zh-Hans":"产品"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Dairy","zh-Hant":"奶類","zh-Hans":"奶类"},
                "cat3Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "prices":[{"supermarketCode":"STORE-A","price":"%s"}],"offers":[]}
                """.formatted(code, code, price);
        return ("[" + product + "]").getBytes(StandardCharsets.UTF_8);
    }
}
