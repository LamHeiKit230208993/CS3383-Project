package com.pricewatch.notifications;

import com.pricewatch.catalog.importer.CatalogJsonParser;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:alerts-j06;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "pricewatch.jobs.enabled=false"
})
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
    void retailerSwitchSurvivesWishlistPauseAndDeleteKeepsTrackingAndHistory() {
        String code = "ALERT-SWITCH";
        replaceLive(code, "12");
        tracking.save(code);
        long id = tracking.start(code, "STORE-A").id();
        tracking.configureTarget(id, new BigDecimal("9"));
        tracking.configureDrop(id, true);
        assertThrows(IllegalArgumentException.class, () -> tracking.setLegacyAlertsEnabled("OTHER", id, false));
        tracking.setLegacyAlertsEnabled(code, id, false);

        now.set(now.get().plusSeconds(60));
        replaceLive(code, "8");
        assertEquals(2, tracking.priceHistory(code).size());
        assertEquals(0, count("TARGET"));
        assertEquals(0, count("DROP"));

        tracking.removeSaved(code);
        var paused = tracking.legacyRules().getFirst();
        assertFalse(paused.enabled());
        assertFalse(paused.alertsEnabled());
        assertEquals(new BigDecimal("9.0000"), paused.targetPrice());
        assertTrue(paused.dropEnabled());
        tracking.save(code);
        var resumed = tracking.legacyRules().getFirst();
        assertTrue(resumed.enabled());
        assertFalse(resumed.alertsEnabled());
        assertTrue(id != resumed.id());
        tracking.setLegacyAlertsEnabled(code, resumed.id(), true);

        now.set(now.get().plusSeconds(60));
        replaceLive(code, "8");
        now.set(now.get().plusSeconds(60));
        replaceLive(code, "7");
        assertEquals(1, count("TARGET"));
        assertEquals(1, count("DROP"));

        tracking.deleteLegacyAlerts(code, resumed.id());
        assertTrue(tracking.legacyRules().isEmpty());
        now.set(now.get().plusSeconds(60));
        replaceLive(code, "6");
        assertEquals(5, tracking.priceHistory(code).size());
        assertEquals(1, count("TARGET"));
        assertEquals(1, count("DROP"));
        assertTrue(trackingRepository.findById(resumed.id()).orElseThrow().isEnabled());
    }

    @Test
    void savedQuietHoursLeaveNewAndPreviouslyDelayedPriceAlertsAvailable() {
        saveHistoricalNotificationSettings();
        now.set(Instant.parse("2026-11-06T15:00:00Z")); // 23:00 Hong Kong.
        replaceLive("ALERT-LEGACY-TIMING", "5.00");
        var period = tracking.start("ALERT-LEGACY-TIMING", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        var target = event("TARGET");
        assertEquals(now.get(), target.getAvailableAt());
        assertTrue(view(target.getId()).available());

        Instant recordedDelay = Instant.parse("2026-11-07T00:00:00Z");
        var oldDrop = eventRepository.save(new AlertEventEntity(trackingRepository.getReferenceById(period.id()),
                null, "DROP", new BigDecimal("5.00"), now.get(), new BigDecimal("6.00"), now.get().minusSeconds(60),
                now.get(), recordedDelay));
        assertTrue(view(oldDrop.getId()).available());
        assertEquals(now.get(), view(oldDrop.getId()).availableAt());
        assertEquals(recordedDelay, eventRepository.findById(oldDrop.getId()).orElseThrow().getAvailableAt());
    }

    @Test
    void historicalSummariesRemainStoredAndReadingAlertsCreatesNoSummary() {
        var historicalSummary = eventRepository.save(new AlertEventEntity(LocalDate.of(2026, 11, 5), 2,
                START.minusSeconds(24 * 60 * 60L)));
        replaceLive("ALERT-READ", "5.00");
        var period = tracking.start("ALERT-READ", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        AlertEventEntity event = event("TARGET");
        alerts.markRead(event.getId());

        now.set(START.plusSeconds(24 * 60 * 60L));
        assertTrue(view(event.getId()).available());
        assertTrue(view(event.getId()).read());

        assertEquals(1, count("SUMMARY"));
        assertEquals(2, eventRepository.findById(historicalSummary.getId()).orElseThrow().getSummaryCount());
        assertNotNull(eventRepository.findById(event.getId()).orElseThrow().getReadAt());
        assertNull(eventRepository.findById(event.getId()).orElseThrow().getSummarizedAt());
    }

    @Test
    void snoozeHidesAnAlertForExactlyTwentyFourHours() {
        saveHistoricalNotificationSettings();
        now.set(Instant.parse("2026-11-06T15:00:00Z"));
        replaceLive("ALERT-HIDDEN", "5.00");
        var period = tracking.start("ALERT-HIDDEN", "STORE-A");
        tracking.configureTarget(period.id(), new BigDecimal("5.00"));
        AlertEventEntity event = event("TARGET");
        alerts.snooze(event.getId());
        Instant due = now.get().plusSeconds(24 * 60 * 60L);
        assertEquals(due, view(event.getId()).snoozedUntil());
        assertFalse(view(event.getId()).available());

        now.set(due.minusMillis(1));
        assertFalse(view(event.getId()).available());
        now.set(due);
        assertTrue(view(event.getId()).available());
        assertEquals(new BigDecimal("5.0000"), view(event.getId()).price());
    }

    private void saveHistoricalNotificationSettings() {
        settingsRepository.save(new NotificationSettingsEntity(
                LocalTime.of(22, 0), LocalTime.of(8, 0), LocalTime.of(7, 45)));
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

    private AlertEventView view(long eventId) {
        return alerts.events(now.get()).stream().filter(event -> event.id() == eventId).findFirst().orElseThrow();
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
