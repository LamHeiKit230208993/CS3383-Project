package com.pricewatch.catalog;

import com.pricewatch.catalog.persistence.DailyRefreshState;
import com.pricewatch.catalog.persistence.DailyRefreshState.Outcome;
import com.pricewatch.catalog.repository.DailyRefreshStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Service
public class DailyCatalogueRefresh {
    private static final Logger log = LoggerFactory.getLogger(DailyCatalogueRefresh.class);
    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");

    private final CatalogService catalog;
    private final DailyRefreshStateRepository states;
    private final Clock clock;
    private final Executor executor;
    private final boolean enabled;

    private final Object importLock = new Object();
    private final AtomicBoolean queued = new AtomicBoolean();
    private volatile LocalDate checkedDay;
    private CompletableFuture<ImportStatus> inFlight;

    DailyCatalogueRefresh(CatalogService catalog, DailyRefreshStateRepository states, Clock clock,
                          @Qualifier("applicationTaskExecutor") Executor executor,
                          @Value("${pricewatch.jobs.enabled:true}") boolean enabled) {
        this.catalog = catalog;
        this.states = states;
        this.clock = clock;
        this.executor = executor;
        this.enabled = enabled;
    }

    public void onUse() {
        if (!enabled || today().equals(checkedDay) || !queued.compareAndSet(false, true)) return;
        try {
            executor.execute(() -> {
                try {
                    checkDailyRefresh();
                } catch (RuntimeException exception) {
                    log.warn("Automatic price refresh did not complete", exception);
                } finally {
                    queued.set(false);
                }
            });
        } catch (RejectedExecutionException exception) {
            queued.set(false);
            log.warn("Automatic price refresh could not be queued", exception);
        }
    }

    public ImportStatus refreshManually() {
        CompletableFuture<ImportStatus> attempt;
        boolean joined;
        synchronized (this) {
            joined = inFlight != null;
            if (!joined) inFlight = new CompletableFuture<>();
            attempt = inFlight;
        }
        if (!joined) {
            synchronized (importLock) {
                refresh(attempt, null);
            }
        }
        return attempt.join();
    }

    public <T> T replaceWithLocalCatalogue(Supplier<T> replacement) {
        synchronized (importLock) {
            return replacement.get();
        }
    }

    public boolean failedToday(ImportStatus accepted) {
        LocalDate day = today();
        return enabled && !protectedOrCurrent(accepted, day) && states.findById(1L)
                .filter(state -> state.refreshDay().equals(day))
                .filter(state -> state.outcome() == Outcome.FAILED
                        || (state.outcome() == Outcome.STARTED && !queued.get()))
                .isPresent();
    }

    private void checkDailyRefresh() {
        synchronized (importLock) {
            LocalDate day = today();
            ImportStatus accepted = catalog.status();
            if (protectedOrCurrent(accepted, day)
                    || states.findById(1L).filter(state -> state.refreshDay().equals(day)).isPresent()) {
                checkedDay = day;
                return;
            }
            CompletableFuture<ImportStatus> attempt;
            synchronized (this) {
                if (inFlight != null) return;
                attempt = new CompletableFuture<>();
                inFlight = attempt;
            }
            refresh(attempt, day);
        }
    }

    private void refresh(CompletableFuture<ImportStatus> attempt, LocalDate automaticDay) {
        try {
            if (automaticDay != null) {
                // Commit the attempt before downloading so a restart cannot issue it twice.
                states.saveAndFlush(new DailyRefreshState(automaticDay, Outcome.STARTED));
                checkedDay = automaticDay;
            }
            ImportStatus accepted = catalog.refreshLive();
            LocalDate acceptedDay = accepted.retrievedAt().atZone(HONG_KONG).toLocalDate();
            try {
                states.saveAndFlush(new DailyRefreshState(acceptedDay, Outcome.ACCEPTED));
            } catch (RuntimeException exception) {
                // The accepted import already records the successful fetch and its retrieval day.
                log.warn("Could not update the daily refresh record", exception);
            }
            checkedDay = acceptedDay;
            attempt.complete(accepted);
        } catch (RuntimeException exception) {
            if (automaticDay != null) {
                try {
                    states.saveAndFlush(new DailyRefreshState(automaticDay, Outcome.FAILED));
                    checkedDay = automaticDay;
                } catch (RuntimeException writeFailure) {
                    exception.addSuppressed(writeFailure);
                }
                log.warn("Automatic price refresh failed", exception);
            }
            attempt.completeExceptionally(exception);
        } finally {
            synchronized (this) {
                inFlight = null;
            }
        }
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), HONG_KONG);
    }

    private static boolean protectedOrCurrent(ImportStatus accepted, LocalDate day) {
        return accepted != null && (!"LIVE".equals(accepted.origin())
                || accepted.retrievedAt().atZone(HONG_KONG).toLocalDate().equals(day));
    }
}
