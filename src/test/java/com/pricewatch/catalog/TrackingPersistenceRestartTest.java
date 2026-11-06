package com.pricewatch.catalog;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.NotificationPolicy;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        long observationCount;
        long alertCount;

        try (ConfigurableApplicationContext firstRun = start(databaseUrl)) {
            CatalogService catalog = firstRun.getBean(CatalogService.class);
            catalog.accept(fixture("10.00"), "LIVE", "test-live");
            TrackingService tracking = firstRun.getBean(TrackingService.class);
            tracking.save("TRACK-RESTART");
            var period = tracking.start("TRACK-RESTART", "STORE-A");
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
            firstRun.getBean(NotificationPolicy.class).update(
                    LocalTime.of(21, 30), LocalTime.of(7, 30), LocalTime.of(7, 45));
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

            NotificationPolicy.Settings settings = reopened.getBean(NotificationPolicy.class).settings();
            assertEquals(LocalTime.of(21, 30), settings.quietStart());
            assertEquals(LocalTime.of(7, 30), settings.quietEnd());
            assertEquals(LocalTime.of(7, 45), settings.summaryTime());
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
        String json = """
                [{"code":"TRACK-RESTART","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"Restart product","zh-Hant":"商品","zh-Hans":"商品"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Noodles","zh-Hant":"麵","zh-Hans":"面"},
                "cat3Name":{"en":"Instant noodles","zh-Hant":"即食麵","zh-Hans":"即食面"},
                "prices":[{"supermarketCode":"STORE-A","price":"%s"}],"offers":[]}]
                """.formatted(price);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
