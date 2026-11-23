package com.pricewatch.catalog;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.TrackingView;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingPersistenceRestartTest {
    @Test
    void wishlistHistoryAlertAndTimingSettingsSurviveApplicationRestart(@TempDir Path directory) {
        String databaseUrl = "jdbc:h2:file:" + directory.resolve("tracking-restart").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        Long trackingId;
        Long targetEventId;
        Long dropEventId;
        Long oldSummaryId;
        long observationCount;
        long alertCount;

        try (ConfigurableApplicationContext firstRun = start(databaseUrl)) {
            CatalogService catalog = firstRun.getBean(CatalogService.class);
            catalog.accept(fixture("10.00"), "LIVE", "test-live");
            TrackingService tracking = firstRun.getBean(TrackingService.class);
            tracking.save("TRACK-RESTART");
            var period = tracking.start("TRACK-RESTART", "STORE-A");
            tracking.startAll("TRACK-RESTART");
            trackingId = period.id();
            tracking.configureDrop(trackingId, true);
            tracking.configureTarget(trackingId, new BigDecimal("8.00"));
            catalog.accept(fixture("7.00"), "LIVE", "test-live");

            List<AlertEventEntity> events = firstRun.getBean(AlertEventRepository.class).findAll();
            targetEventId = events.stream().filter(event -> "TARGET".equals(event.getType()))
                    .findFirst().orElseThrow().getId();
            dropEventId = events.stream().filter(event -> "DROP".equals(event.getType()))
                    .findFirst().orElseThrow().getId();
            firstRun.getBean(AlertService.class).snooze(targetEventId);
            firstRun.getBean(AlertService.class).markRead(dropEventId);
            firstRun.getBean(NotificationSettingsRepository.class).save(new NotificationSettingsEntity(
                    LocalTime.of(21, 30), LocalTime.of(7, 30), LocalTime.of(7, 45)));
            oldSummaryId = firstRun.getBean(AlertEventRepository.class).save(new AlertEventEntity(
                    LocalDate.of(2026, 11, 5), 2, events.getFirst().getCreatedAt().minusSeconds(86_400))).getId();
            observationCount = firstRun.getBean(PriceObservationRepository.class).count();
            alertCount = firstRun.getBean(AlertEventRepository.class).count();
        }

        try (ConfigurableApplicationContext reopened = start(databaseUrl)) {
            CatalogService catalog = reopened.getBean(CatalogService.class);
            TrackingService tracking = reopened.getBean(TrackingService.class);
            assertEquals(1, tracking.savedProducts().size());
            var restored = tracking.trackingPeriods().stream().filter(item -> item.id() == trackingId)
                    .findFirst().orElseThrow();
            assertEquals(2, restored.observations().size());
            assertEquals("TRACK-RESTART", tracking.allRetailersProducts().getFirst().code());
            assertEquals(new BigDecimal("7.0000"), restored.observations().getFirst().price());

            AlertEventEntity targetEvent = reopened.getBean(AlertEventRepository.class)
                    .findById(targetEventId).orElseThrow();
            assertEquals("TARGET", targetEvent.getType());
            assertNotNull(targetEvent.getSnoozedUntil());
            assertTrue(reopened.getBean(AlertEventRepository.class).findAll().stream()
                    .anyMatch(event -> "DROP".equals(event.getType())));
            AlertEventEntity dropEvent = reopened.getBean(AlertEventRepository.class)
                    .findById(dropEventId).orElseThrow();
            assertNotNull(dropEvent.getReadAt());
            AlertEventEntity oldSummary = reopened.getBean(AlertEventRepository.class)
                    .findById(oldSummaryId).orElseThrow();
            assertEquals(LocalDate.of(2026, 11, 5), oldSummary.getSummaryDate());
            assertEquals(2, oldSummary.getSummaryCount());

            var settings = reopened.getBean(NotificationSettingsRepository.class).findById(1L).orElseThrow();
            assertEquals(LocalTime.of(21, 30), settings.getQuietStart());
            assertEquals(LocalTime.of(7, 30), settings.getQuietEnd());
            assertEquals(LocalTime.of(7, 45), settings.getSummaryTime());
            assertNotNull(reopened.getBean(PriceObservationRepository.class)
                    .findTopByTracking_IdOrderByObservedAtDescIdDesc(trackingId).orElse(null));
            assertTrue(reopened.getBean(TrackingRepository.class).findById(trackingId).isPresent());
            assertTrue(reopened.getBean(SavedProductRepository.class).findById("TRACK-RESTART").isPresent());

            catalog.accept(fixture("7.00"), "LIVE", "test-live");
            assertEquals(observationCount, reopened.getBean(PriceObservationRepository.class).count(),
                    "An unchanged LIVE re-import after restart does not add an observation.");
            assertEquals(alertCount, reopened.getBean(AlertEventRepository.class).count(),
                    "Persisted target arming and event identity prevent a repeated alert after restart.");
        }
    }

    @Test
    void oldWishlistAndTrackingRowsUpgradeWithoutLosingHistoryOrRules(@TempDir Path directory) {
        String databaseUrl = "jdbc:h2:file:" + directory.resolve("legacy-wishlist").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        String saved = product("LEGACY-SAVED", store("STORE-A", "5.00"));
        String tracked = product("LEGACY-TRACKED", store("STORE-A", "7.00"));
        String flagged = product("LEGACY-FLAGGED", "");
        long periodId;

        try (ConfigurableApplicationContext legacyRun = start(databaseUrl)) {
            CatalogService catalog = legacyRun.getBean(CatalogService.class);
            catalog.accept(catalogue(saved, tracked, flagged), "LIVE", "test-live");
            var jdbc = legacyRun.getBean(JdbcTemplate.class);
            jdbc.update("insert into wishlist_entries (product_code, saved_at) values (?, current_timestamp)", "LEGACY-SAVED");
            TrackingService tracking = legacyRun.getBean(TrackingService.class);
            periodId = tracking.start("LEGACY-TRACKED", "STORE-A").id();
            tracking.configureTarget(periodId, new BigDecimal("8.00"));
            tracking.configureDrop(periodId, true);
            tracking.startAll("LEGACY-FLAGGED");
            legacyRun.getBean(ProductTargetService.class).setTarget("LEGACY-SAVED", new BigDecimal("4.00"));
            jdbc.execute("alter table product_tracking_periods drop column wishlist_paused");
            jdbc.execute("alter table product_tracking_periods drop column alerts_paused");
            jdbc.execute("alter table product_target_rules drop column paused");
        }

        try (ConfigurableApplicationContext upgraded = start(databaseUrl)) {
            TrackingService tracking = upgraded.getBean(TrackingService.class);
            var codes = Set.of("LEGACY-SAVED", "LEGACY-TRACKED", "LEGACY-FLAGGED");
            assertEquals(codes, tracking.savedProducts().stream()
                    .map(entry -> entry.product().code()).collect(Collectors.toSet()));
            assertEquals(codes, tracking.allRetailersProducts().stream()
                    .map(TrackingService.AllRetailersView::code).collect(Collectors.toSet()));
            assertEquals(1, tracking.trackingPeriods().size(), "Upgrade waits for an accepted live refresh.");
            var existing = tracking.trackingPeriods().getFirst();
            assertEquals(periodId, existing.id());
            assertTrue(existing.dropEnabled());
            assertTrue(existing.alertsEnabled());
            assertEquals(new BigDecimal("8.0000"), existing.targetPrice());
            assertEquals(new BigDecimal("7.0000"), existing.observations().getFirst().price());
            assertEquals(1, upgraded.getBean(AlertEventRepository.class).count());
            assertEquals(1, upgraded.getBean(PriceObservationRepository.class).count());
            var targets = upgraded.getBean(ProductTargetService.class);
            assertEquals(new ProductTargetService.RuleView("LEGACY-SAVED", new BigDecimal("4.0000"), true),
                    targets.rule("LEGACY-SAVED").orElseThrow());
            targets.setEnabled("LEGACY-SAVED", false);
            tracking.setLegacyAlertsEnabled("LEGACY-TRACKED", periodId, false);

            var feed = catalogue(saved, product("LEGACY-TRACKED",
                    store("STORE-A", "7.00") + "," + store("STORE-B", "6.00")), flagged);
            CatalogService catalog = upgraded.getBean(CatalogService.class);
            catalog.accept(feed, "SAMPLE", "test-sample");
            assertEquals(1, tracking.trackingPeriods().size());
            catalog.accept(feed, "LIVE", "test-live");
            assertEquals(3, tracking.trackingPeriods().stream().filter(TrackingView::enabled).count());
            assertEquals(3, upgraded.getBean(PriceObservationRepository.class).count());
            assertEquals(1, upgraded.getBean(AlertEventRepository.class).count());
            tracking.removeSaved("LEGACY-TRACKED");
        }

        try (ConfigurableApplicationContext reopened = start(databaseUrl)) {
            TrackingService tracking = reopened.getBean(TrackingService.class);
            assertEquals(2, tracking.savedProducts().size(), "A removed product must not reappear after restart.");
            assertEquals(1, tracking.trackingPeriods().stream().filter(TrackingView::enabled).count());
            assertEquals(2, tracking.priceHistory("LEGACY-TRACKED").size());
            assertFalse(tracking.legacyRules().getFirst().alertsEnabled());
            assertFalse(reopened.getBean(ProductTargetService.class).rule("LEGACY-SAVED").orElseThrow().enabled());
        }
    }

    private ConfigurableApplicationContext start(String databaseUrl) {
        return new SpringApplicationBuilder(PricewatchApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + databaseUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.jpa.open-in-view=false",
                        "--spring.main.banner-mode=off",
                        "--spring.main.web-application-type=none",
                        "--pricewatch.jobs.enabled=false");
    }

    private static byte[] fixture(String price) {
        return catalogue(product("TRACK-RESTART", store("STORE-A", price)));
    }

    private static byte[] catalogue(String... products) {
        return ("[" + String.join(",", products) + "]").getBytes(StandardCharsets.UTF_8);
    }

    private static String store(String code, String price) {
        return "{\"supermarketCode\":\"%s\",\"price\":\"%s\"}".formatted(code, price);
    }

    private static String product(String code, String prices) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"Restart product","zh-Hant":"商品","zh-Hans":"商品"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Noodles","zh-Hant":"麵","zh-Hans":"面"},
                "cat3Name":{"en":"Instant noodles","zh-Hant":"即食麵","zh-Hans":"即食面"},
                "prices":[%s],"offers":[]}
                """.formatted(code, prices);
    }
}
