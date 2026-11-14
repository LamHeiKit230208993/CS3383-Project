package com.pricewatch.catalog;

import com.pricewatch.catalog.importer.CatalogJsonParser;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.importer.ConsumerCouncilSourceClient;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.PriceObservationView;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.TrackingView;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tracking-j05;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"
})
@AutoConfigureMockMvc
class TrackingServiceTest {
    private static final java.time.Instant START = java.time.Instant.parse("2026-11-06T04:00:00Z");
    @Autowired private CatalogService catalog;
    @Autowired private TrackingService tracking;
    @Autowired private TrackingRepository trackingRepository;
    @Autowired private PriceObservationRepository observationRepository;
    @Autowired private SavedProductRepository savedRepository;
    @Autowired private AlertEventRepository alertEventRepository;
    @Autowired private NotificationSettingsRepository notificationSettingsRepository;
    @Autowired private AcceptedImportRepository acceptedImportRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;
    @Autowired private CatalogJsonParser parser;
    @Autowired private CatalogReplacementService replacementService;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private Clock appClock;
    @Autowired private MockMvc mvc;
    private final AtomicReference<java.time.Instant> now = new AtomicReference<>(START);

    @BeforeEach
    void clearDatabase() {
        now.set(START);
        when(appClock.instant()).thenAnswer(invocation -> now.get());
        alertEventRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        savedRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        acceptedImportRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        notificationSettingsRepository.deleteAllInBatch();
    }

    @Test
    void productWideTrackingEnrollsNewLiveRetailersAndStopsTogether() throws Exception {
        String first = "{\"supermarketCode\":\"STORE-A\",\"price\":\"10.00\"}";
        String second = "{\"supermarketCode\":\"STORE-B\",\"price\":\"4.00\"}";
        catalog.accept(json(product("TRACK-ALL", "[" + first + "]")), "LIVE", "fixture-live");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/tracking/start-all").param("code", "TRACK-ALL"))
                .andExpect(status().is3xxRedirection());
        long firstId = tracking.trackingPeriods().getFirst().id();
        tracking.configureDrop(firstId, true);
        tracking.startAll("TRACK-ALL");
        assertEquals(1, tracking.trackingPeriods().size());

        byte[] expanded = json(product("TRACK-ALL", "[" + first + "," + second + "]"));
        catalog.accept(expanded, "SAMPLE", "fixture-sample");
        assertEquals(1, tracking.trackingPeriods().size());
        catalog.accept(expanded, "LIVE", "fixture-live");
        catalog.accept(expanded, "LIVE", "fixture-live");
        assertEquals(2, tracking.trackingPeriods().size());
        assertTrue(tracking.trackingPeriods().stream().allMatch(period -> period.observations().size() == 1));
        assertEquals(0, alertEventRepository.count(), "Different retailers cannot form a drop comparison.");
        assertThrows(IllegalArgumentException.class, () -> tracking.stop(firstId));
        mvc.perform(get("/saved")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stop all retailers")));

        tracking.stopAll("TRACK-ALL");
        String third = "{\"supermarketCode\":\"STORE-C\",\"price\":\"3.00\"}";
        catalog.accept(json(product("TRACK-ALL", "[" + first + "," + second + "," + third + "]")),
                "LIVE", "fixture-live");
        assertEquals(2, tracking.trackingPeriods().size());
        assertTrue(tracking.trackingPeriods().stream().noneMatch(TrackingView::enabled));
        assertTrue(tracking.allRetailersProducts().isEmpty());
    }

    @Test
    void savedWishlistIsSeparateAndOnlyLiveImportsAddChangedObservations() {
        catalog.accept(json(product("TRACK-1", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5.00\"},"
                + "{\"supermarketCode\":\"STORE-B\",\"price\":\"9.00\"}]")), "SAMPLE", "fixture");
        tracking.save("TRACK-1");
        TrackingView period = tracking.start("TRACK-1", "STORE-A");

        assertEquals(1, tracking.savedProducts().size());
        assertTrue(period.observations().isEmpty(), "A sample must not seed live history.");
        assertEquals(1, tracking.trackingPeriods().stream().filter(TrackingView::enabled).count());

        catalog.accept(json(product("TRACK-1", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5.0000\"},"
                + "{\"supermarketCode\":\"STORE-B\",\"price\":\"9.00\"}]")), "LIVE", "fixture-live");
        assertEquals(1, observationRepository.count());
        assertEquals("fixture-live", observationRepository.findAll().getFirst().getSourceUrl());

        catalog.accept(json(product("TRACK-1", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5\"},"
                + "{\"supermarketCode\":\"STORE-B\",\"price\":\"9.00\"}]")), "LIVE", "fixture-live");
        assertEquals(1, observationRepository.count(), "A consecutive identical price is skipped.");

        catalog.accept(json(product("TRACK-1", "[{\"supermarketCode\":\"STORE-A\",\"price\":null},"
                + "{\"supermarketCode\":\"STORE-B\",\"price\":\"9.00\"}]")), "LIVE", "fixture-live");
        catalog.accept(json(product("TRACK-1", "[{\"supermarketCode\":\"STORE-A\",\"price\":null},"
                + "{\"supermarketCode\":\"STORE-B\",\"price\":\"9.00\"}]")), "LIVE", "fixture-live");
        assertEquals(2, observationRepository.count(), "One null gap is recorded; repeated gaps are skipped.");

        catalog.accept(json(product("TRACK-1", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"4.00\"},"
                + "{\"supermarketCode\":\"STORE-B\",\"price\":\"9.00\"}]")), "LIVE", "fixture-live");
        List<PriceObservationView> history = tracking.trackingPeriods().getFirst().observations();
        assertEquals(3, history.size());
        assertEquals(new BigDecimal("4.0000"), history.getFirst().price());
        assertNull(history.get(1).price());
        assertEquals(new BigDecimal("5.0000"), history.getLast().price());
    }

    @Test
    void savedPageAndAlertPageRenderWithTheirOwnScenes() throws Exception {
        catalog.importSample();

        mvc.perform(get("/saved").param("code", "P000000124"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Save to wishlist")));

        mvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Price alerts, on your schedule.")));
    }

    @Test
    void stoppingKeepsOldHistoryAndReenableCreatesANewSeededPeriod() {
        catalog.accept(json(product("TRACK-2", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"10.00\"}]")),
                "SAMPLE", "fixture-sample");
        tracking.save("TRACK-2");
        TrackingView oldPeriod = tracking.start("TRACK-2", "STORE-A");
        tracking.configureDrop(oldPeriod.id(), true);

        catalog.accept(json(product("TRACK-2", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"10.00\"}]")),
                "LIVE", "fixture-live");
        assertEquals(1, tracking.trackingPeriods().getFirst().observations().size());
        tracking.stop(oldPeriod.id());
        catalog.accept(json(product("TRACK-2", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"8.00\"}]")),
                "LIVE", "fixture-live");

        TrackingView newPeriod = tracking.start("TRACK-2", "STORE-A");
        assertNotEquals(oldPeriod.id(), newPeriod.id());
        assertEquals(1, newPeriod.observations().size(), "Only the current live catalogue seeds the new period.");
        tracking.configureDrop(newPeriod.id(), true);
        catalog.accept(json(product("TRACK-2", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"7.00\"}]")),
                "LIVE", "fixture-live");

        assertEquals(1, tracking.trackingPeriods().stream().filter(period -> period.id() == oldPeriod.id())
                .findFirst().orElseThrow().observations().size());
        assertEquals(2, tracking.trackingPeriods().getFirst().observations().size());
        assertEquals(1, alertEventRepository.findAll().stream().filter(event -> "DROP".equals(event.getType())).count());

        tracking.deleteHistory(oldPeriod.id());
        assertTrue(tracking.trackingPeriods().stream().filter(period -> period.id() == oldPeriod.id())
                .findFirst().orElseThrow().observations().isEmpty());
        assertEquals(1, tracking.savedProducts().size(), "Deleting history does not unsave the product.");
    }

    @Test
    void concurrentStartRequestsCreateOnlyOneActivePeriod() throws Exception {
        catalog.accept(json(product("TRACK-3", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"3.00\"}]")),
                "SAMPLE", "fixture");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<TrackingView> first = executor.submit(() -> startTogether(ready, start));
            Future<TrackingView> second = executor.submit(() -> startTogether(ready, start));
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            assertEquals(first.get(10, java.util.concurrent.TimeUnit.SECONDS).id(),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS).id());
            assertEquals(1, trackingRepository.findAllByEnabledTrueOrderByStartedAtDescIdDesc().size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @Test
    void failedImportRollsBackItsObservationAndAlertAfterTheyWereWritten() {
        catalog.accept(json(product("TRACK-4", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"10.00\"}]")),
                "SAMPLE", "fixture");
        TrackingView period = tracking.start("TRACK-4", "STORE-A");
        catalog.accept(json(product("TRACK-4", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"10.00\"}]")),
                "LIVE", "fixture-live");
        tracking.configureDrop(period.id(), true);
        var priorStatus = catalog.status();
        long priorObservations = observationRepository.count();
        long priorAlerts = alertEventRepository.count();
        var replacement = parser.parse(json(product("TRACK-4", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"9.00\"}]")));
        jdbc.execute("alter table accepted_import add constraint ck_tracking_import_failure check (checksum <> 'FORCE-ROLLBACK')");
        try {
            assertThrows(DataIntegrityViolationException.class,
                    () -> replacementService.replace(replacement, "LIVE", "fixture-live", appClock.instant(),
                            "FORCE-ROLLBACK"));
            assertEquals(priorStatus, catalog.status());
            assertEquals(priorObservations, observationRepository.count());
            assertEquals(priorAlerts, alertEventRepository.count());
            assertEquals(new BigDecimal("10.0000"), catalog.find("TRACK-4").listings().getFirst().price());
        } finally {
            jdbc.execute("alter table accepted_import drop constraint ck_tracking_import_failure");
        }
    }

    @Test
    void openingHistoryPurgesObservationsOlderThanNinetyDays() {
        catalog.accept(json(product("TRACK-5", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5.00\"}]")),
                "SAMPLE", "fixture");
        TrackingView period = tracking.start("TRACK-5", "STORE-A");
        observationRepository.save(new com.pricewatch.tracking.persistence.PriceObservationEntity(
                trackingRepository.findById(period.id()).orElseThrow(), new BigDecimal("5.0000"),
                appClock.instant().minusSeconds(91L * 24 * 60 * 60), "fixture", "fixture-checksum"));

        assertTrue(tracking.trackingPeriods().getFirst().observations().isEmpty());
    }

    @Test
    void unchangedImportAfterRetentionDoesNotCreateHistoryAndChangedPriceStartsNewBaseline() {
        catalog.accept(json(product("TRACK-6", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5.00\"}]")),
                "LIVE", "fixture-live");
        TrackingView period = tracking.start("TRACK-6", "STORE-A");
        tracking.configureDrop(period.id(), true);
        assertEquals(1, observationRepository.count());

        now.set(START.plusSeconds(91L * 24 * 60 * 60));
        catalog.accept(json(product("TRACK-6", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5.00\"}]")),
                "LIVE", "fixture-live");
        assertEquals(0, observationRepository.count(), "An unchanged current price does not repopulate expired history.");

        now.set(now.get().plusSeconds(1));
        catalog.accept(json(product("TRACK-6", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"4.00\"}]")),
                "LIVE", "fixture-live");
        assertEquals(1, observationRepository.count());
        assertEquals(0, alertEventRepository.findAll().stream().filter(event -> "DROP".equals(event.getType())).count(),
                "A post-retention price starts a new baseline; there is no retained adjacent price to compare.");
    }

    @Test
    void targetDoesNotUseSampleOrExpiredHistory() {
        catalog.accept(json(product("TRACK-7", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"5.00\"}]")),
                "LIVE", "fixture-live");
        TrackingView period = tracking.start("TRACK-7", "STORE-A");
        assertEquals(1, observationRepository.count());

        catalog.accept(json(product("TRACK-7", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"1.00\"}]")),
                "SAMPLE", "fixture-sample");
        tracking.configureTarget(period.id(), new BigDecimal("6.00"));
        assertEquals(0, alertEventRepository.findAll().stream().filter(event -> "TARGET".equals(event.getType())).count(),
                "A sample replacement must not make a previous live observation current.");

        var oldLiveImport = parser.parse(json(product("TRACK-7", "[{\"supermarketCode\":\"STORE-A\",\"price\":\"4.00\"}]")));
        var expiredAt = now.get().minusSeconds(91L * 24 * 60 * 60);
        replacementService.replace(oldLiveImport, "LIVE", "fixture-live", expiredAt, "expired-live");
        assertEquals(1, observationRepository.count(), "A stale changed feed is not recorded as a new observation.");
        tracking.configureTarget(period.id(), new BigDecimal("7.00"));
        assertEquals(0, alertEventRepository.findAll().stream().filter(event -> "TARGET".equals(event.getType())).count(),
                "A target cannot use an accepted LIVE catalogue older than the retention window.");
    }

    private TrackingView startTogether(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent tracking requests.");
        }
        return tracking.start("TRACK-3", "STORE-A");
    }

    private static byte[] json(String product) {
        return ("[" + product + "]").getBytes(StandardCharsets.UTF_8);
    }

    private static String product(String code, String prices) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"商品","zh-Hans":"商品"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Noodles","zh-Hant":"麵","zh-Hans":"面"},
                "cat3Name":{"en":"Instant noodles","zh-Hant":"即食麵","zh-Hans":"即食面"},
                "prices":%s,"offers":[]}
                """.formatted(code, code, prices).trim();
    }
}
