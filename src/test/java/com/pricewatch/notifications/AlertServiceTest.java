package com.pricewatch.notifications;

import com.pricewatch.catalog.importer.CatalogJsonParser;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:alerts-j06;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"
})
@AutoConfigureMockMvc
class AlertServiceTest {
    private static final Instant START = Instant.parse("2026-11-06T04:00:00Z");

    @Autowired private AlertService alerts;
    @Autowired private TrackingService tracking;
    @Autowired private CatalogJsonParser parser;
    @Autowired private CatalogReplacementService replacement;
    @Autowired private TrackingRepository trackingRepository;
    @Autowired private PriceObservationRepository observationRepository;
    @Autowired private SavedProductRepository savedRepository;
    @Autowired private AlertEventRepository eventRepository;
    @Autowired private NotificationSettingsRepository settingsRepository;
    @Autowired private AcceptedImportRepository acceptedRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private MockMvc mvc;
    @MockitoBean private Clock clock;

    private final AtomicReference<Instant> now = new AtomicReference<>(START);

    @BeforeEach
    void clearState() {
        now.set(START);
        when(clock.instant()).thenAnswer(invocation -> now.get());
        eventRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        savedRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        acceptedRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
        var policy = settings();
        policy.update(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
    }

    @Test
    void targetUsesCurrentLivePriceAndOnlyRepeatsAfterItRisesAboveTarget() {
        replaceLive("ALERT-TARGET", "10.00");
        var period = tracking.start("ALERT-TARGET", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("10.00"));
        assertEquals(1, count("TARGET"), "The current known price meets the inclusive target.");

        replaceLive("ALERT-TARGET", "10.00");
        assertEquals(1, count("TARGET"), "An unchanged price cannot repeat the alert.");
        now.set(now.get().plusSeconds(60));
        replaceLive("ALERT-TARGET", "11.00");
        now.set(now.get().plusSeconds(60));
        replaceLive("ALERT-TARGET", "10.00");
        assertEquals(2, count("TARGET"), "A price above the target rearms a later qualifying price.");
    }

    @Test
    void currentLivePriceCanTriggerTargetAfterHistoryExpiresWithoutRestoringOldRows() {
        replaceLive("ALERT-RETAINED", "10.00");
        var period = tracking.start("ALERT-RETAINED", "STORE-A");
        assertEquals(1, observationRepository.count());

        now.set(START.plusSeconds(91L * 24 * 60 * 60));
        replaceLive("ALERT-RETAINED", "10.00");
        assertEquals(0, observationRepository.count(), "The unchanged price must not recreate expired history.");

        tracking.configureTarget(period.id(), new BigDecimal("11.00"));

        assertEquals(1, count("TARGET"));
        assertNull(event("TARGET").getObservationId(), "A current catalogue price is evidence without a retained history row.");
        assertEquals(new BigDecimal("10.0000"), event("TARGET").getPrice());
        assertEquals(now.get(), event("TARGET").getObservedAt());
        replaceLive("ALERT-RETAINED", "10.00");
        assertEquals(1, count("TARGET"), "An unchanged refresh cannot repeat the armed target event.");
        assertEquals(0, observationRepository.count());
    }

    @Test
    void dropEventKeepsBothPricesTheirRetrievalTimesAndGenerationTime() {
        replaceLive("ALERT-DROP", "10.00");
        var period = tracking.start("ALERT-DROP", "STORE-A");
        var previousAt = observationRepository.findAll().getFirst().getObservedAt();
        tracking.configureDrop(period.id(), true);
        now.set(now.get().plusSeconds(60));
        replaceLive("ALERT-DROP", "8.00");

        AlertEventEntity event = event("DROP");
        assertEquals(new BigDecimal("10.0000"), event.getPreviousPrice());
        assertEquals(previousAt, event.getPreviousObservedAt());
        assertEquals(new BigDecimal("8.0000"), event.getPrice());
        assertEquals(now.get(), event.getObservedAt());
        assertEquals(now.get(), event.getCreatedAt(), "Generation time is recorded independently.");
        assertTrue(event.getPreviousObservedAt().isBefore(event.getObservedAt()));
    }

    @Test
    void summariesExcludeSnoozedEventsUntilDueAndConcurrentRunsCreateOneSummary() throws Exception {
        replaceLive("ALERT-SUMMARY", "5.00");
        var period = tracking.start("ALERT-SUMMARY", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        AlertEventEntity event = event("TARGET");
        alerts.snooze(event.getId());
        Instant due = eventRepository.findById(event.getId()).orElseThrow().getSnoozedUntil();

        alerts.processDue(due.minusMillis(1));
        assertEquals(0, count("SUMMARY"), "A snoozed event must not enter the morning summary.");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> processTogether(ready, start, due));
            var second = executor.submit(() -> processTogether(ready, start, due));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(1, count("SUMMARY"));
        assertEquals(due, eventRepository.findById(event.getId()).orElseThrow().getSummarizedAt());
        assertEquals(1, count("SUMMARY"), "A second processing pass cannot duplicate today's summary.");
    }

    @Test
    void summaryScheduledDuringQuietHoursRunsWhenQuietHoursEnd() {
        replaceLive("ALERT-QUIET-SUMMARY", "5.00");
        var period = tracking.start("ALERT-QUIET-SUMMARY", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        now.set(Instant.parse("2026-11-06T13:00:00Z")); // 21:00 Hong Kong, before quiet hours.
        settings().update(LocalTime.of(22, 0), LocalTime.of(8, 0), LocalTime.of(23, 0));

        Instant beforeQuietEnd = Instant.parse("2026-11-06T23:59:00Z"); // 07:59 Hong Kong.
        Instant quietEnd = Instant.parse("2026-11-07T00:00:00Z"); // 08:00 Hong Kong.
        alerts.processDue(beforeQuietEnd);
        assertEquals(0, count("SUMMARY"));

        alerts.processDue(quietEnd);

        assertEquals(1, count("SUMMARY"));
        assertEquals(quietEnd, event("SUMMARY").getCreatedAt());
    }

    @Test
    void readAlertsAreNotCountedInMorningSummary() {
        replaceLive("ALERT-READ", "5.00");
        var period = tracking.start("ALERT-READ", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        AlertEventEntity event = event("TARGET");
        alerts.markRead(event.getId());

        alerts.processDue(now.get().plusSeconds(24 * 60 * 60L));

        assertEquals(0, count("SUMMARY"));
        assertNotNull(eventRepository.findById(event.getId()).orElseThrow().getReadAt());
        assertNull(eventRepository.findById(event.getId()).orElseThrow().getSummarizedAt());
        assertTrue(alerts.events(now.get()).stream().filter(view -> view.id() == event.getId())
                .findFirst().orElseThrow().available());
    }

    @Test
    void pendingSnoozedAlertShowsGenericNoticeUntilItBecomesAvailable() throws Exception {
        replaceLive("ALERT-HIDDEN", "5.00");
        var period = tracking.start("ALERT-HIDDEN", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        AlertEventEntity event = event("TARGET");
        alerts.snooze(event.getId());

        now.set(now.get().plusSeconds(60));
        String waitingPage = mvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alert is waiting for quiet hours or snooze to end")))
                .andReturn().getResponse().getContentAsString();
        assertFalse(waitingPage.contains("Target reached for ALERT-HIDDEN at STORE-A."));
        assertFalse(waitingPage.contains("HK$5.00"));

        now.set(eventRepository.findById(event.getId()).orElseThrow().getSnoozedUntil());
        mvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Target reached for ALERT-HIDDEN at STORE-A.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HK$5.00")));
    }

    private NotificationPolicy settings() {
        return new NotificationPolicy(settingsRepository);
    }

    private void replaceLive(String code, String price) {
        var parsed = parser.parse(json(product(code, price)));
        replacement.replace(parsed, "LIVE", "fixture-live", now.get(), "checksum-" + code + "-" + price);
    }

    private long count(String type) {
        return eventRepository.findAll().stream().filter(event -> type.equals(event.getType())).count();
    }

    private AlertEventEntity event(String type) {
        return eventRepository.findAll().stream().filter(event -> type.equals(event.getType())).findFirst().orElseThrow();
    }

    private void processTogether(CountDownLatch ready, CountDownLatch start, Instant due) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Summary start timed out.");
            alerts.processDue(due);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Summary processing was interrupted.", exception);
        }
    }

    private static byte[] json(String product) {
        return ("[" + product + "]").getBytes(StandardCharsets.UTF_8);
    }

    private static String product(String code, String price) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"商品","zh-Hans":"商品"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Noodles","zh-Hant":"麵","zh-Hans":"面"},
                "cat3Name":{"en":"Instant noodles","zh-Hant":"即食麵","zh-Hans":"即食面"},
                "prices":[{"supermarketCode":"STORE-A","price":"%s"}],"offers":[]}
                """.formatted(code, code, price).trim();
    }
}
