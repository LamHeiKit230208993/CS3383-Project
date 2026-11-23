package com.pricewatch.catalog;

import com.pricewatch.catalog.importer.ConsumerCouncilSourceClient;
import com.pricewatch.catalog.persistence.DailyRefreshState;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.DailyRefreshStateRepository;
import com.pricewatch.tracking.DemoHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:daily-refresh;DB_CLOSE_DELAY=-1")
class DailyCatalogueRefreshTest {
    private static final Instant BEFORE_MIDNIGHT = Instant.parse("2026-11-23T15:59:00Z");
    private static final Instant AFTER_MIDNIGHT = Instant.parse("2026-11-23T16:01:00Z");

    @Autowired private MutableClock clock;
    @Autowired private CatalogService catalog;
    @Autowired private AcceptedImportRepository imports;
    @Autowired private DailyRefreshStateRepository states;
    @Autowired private DemoHistoryService demo;
    @MockitoBean private ConsumerCouncilSourceClient source;
    private byte[] prices;

    @BeforeEach
    void setup() throws IOException {
        clock.current = BEFORE_MIDNIGHT;
        states.deleteAllInBatch();
        imports.deleteAllInBatch();
        try (var input = new ClassPathResource("samples/pricewatch-products.json").getInputStream()) {
            prices = input.readAllBytes();
        }
        when(source.sourceUrl()).thenReturn("https://example.test/prices");
        when(source.fetch()).thenReturn(prices);
    }

    @Test
    void checksOncePerHongKongDayAcrossCoordinatorRestart() {
        var running = coordinator();
        running.onUse();
        running.onUse();
        assertEquals(BEFORE_MIDNIGHT, catalog.status().retrievedAt());

        var restarted = coordinator();
        restarted.onUse();
        verify(source).fetch();

        clock.current = AFTER_MIDNIGHT;
        restarted.onUse();
        assertEquals(AFTER_MIDNIGHT, catalog.status().retrievedAt());
        verify(source, times(2)).fetch();
    }

    @Test
    void failedAttemptKeepsCatalogueAndSurvivesRestartUntilManualRecovery() {
        var running = coordinator();
        var original = running.refreshManually();
        clock.current = AFTER_MIDNIGHT;
        when(source.fetch()).thenThrow(new IllegalStateException("Fixture feed unavailable"));
        running.onUse();
        assertEquals(original, catalog.status());
        assertTrue(running.failedToday(catalog.status()));

        var restarted = coordinator();
        restarted.onUse();
        assertTrue(restarted.failedToday(catalog.status()));
        verify(source, times(2)).fetch();

        doReturn(prices).when(source).fetch();
        assertEquals(AFTER_MIDNIGHT, restarted.refreshManually().retrievedAt());
        assertFalse(restarted.failedToday(catalog.status()));
        coordinator().onUse();
        verify(source, times(3)).fetch();
    }

    @Test
    void sampleAndDemoStayUntilManualLiveRefresh() {
        var running = coordinator();
        running.replaceWithLocalCatalogue(catalog::importSample);
        running.onUse();
        assertEquals("SAMPLE", catalog.status().origin());

        clock.current = AFTER_MIDNIGHT;
        coordinator().replaceWithLocalCatalogue(demo::seed);
        coordinator().onUse();
        assertEquals("DEMO", catalog.status().origin());
        verify(source, never()).fetch();

        assertEquals("LIVE", running.refreshManually().origin());
        running.onUse();
        verify(source).fetch();
    }

    @Test
    void interruptedAttemptIsNotRepeatedAndAnAcceptedImportClearsTheWarning() {
        states.saveAndFlush(new DailyRefreshState(LocalDate.of(2026, 11, 23), DailyRefreshState.Outcome.STARTED));
        var restarted = coordinator();
        restarted.onUse();
        verify(source, never()).fetch();
        assertTrue(restarted.failedToday(catalog.status()));

        // The catalogue transaction may have committed before the attempt state was updated.
        var accepted = catalog.refreshLive();
        assertFalse(restarted.failedToday(accepted));
    }

    @Test
    void manualRequestSharesInFlightFetchAndLocalReplacementRunsAfterIt() throws Exception {
        var enteredFeed = new CountDownLatch(1);
        var releaseFeed = new CountDownLatch(1);
        when(source.fetch()).thenAnswer(invocation -> {
            enteredFeed.countDown();
            assertTrue(releaseFeed.await(5, TimeUnit.SECONDS), "The test releases the fixture response");
            return prices;
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var running = new DailyCatalogueRefresh(catalog, states, clock, executor, true);
            running.onUse();
            assertTrue(enteredFeed.await(5, TimeUnit.SECONDS), "The background call reaches the feed");
            var manual = new FutureTask<>(running::refreshManually);
            Thread manualThread = Thread.ofPlatform().daemon().start(manual);
            var sample = new FutureTask<ImportStatus>(() -> running.replaceWithLocalCatalogue(catalog::importSample));
            Thread sampleThread = Thread.ofPlatform().daemon().start(sample);
            try {
                awaitState(manualThread, Thread.State.WAITING);
                awaitState(sampleThread, Thread.State.BLOCKED);
            } finally {
                releaseFeed.countDown();
            }

            assertEquals("LIVE", manual.get(5, TimeUnit.SECONDS).origin());
            assertEquals("SAMPLE", sample.get(5, TimeUnit.SECONDS).origin());
            assertEquals("SAMPLE", catalog.status().origin());
            verify(source).fetch();
        }
    }

    private DailyCatalogueRefresh coordinator() {
        return new DailyCatalogueRefresh(catalog, states, clock, Runnable::run, true);
    }

    private static void awaitState(Thread thread, Thread.State state) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (thread.getState() != state && thread.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertEquals(state, thread.getState());
    }

    @TestConfiguration
    static class ClockFixture {
        @Bean
        @Primary
        MutableClock mutableClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        private volatile Instant current = BEFORE_MIDNIGHT;

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
        @Override public Instant instant() { return current; }
    }
}
