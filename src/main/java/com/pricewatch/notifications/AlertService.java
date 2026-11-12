package com.pricewatch.notifications;

import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
public class AlertService {
    public static final String TARGET = "TARGET";
    public static final String DROP = "DROP";
    private static final String SUMMARY = "SUMMARY";
    private static final List<String> PRICE_ALERTS = List.of(TARGET, DROP);
    private static final long SNOOZE_HOURS = 24;

    private final AlertEventRepository eventRepository;
    private final NotificationPolicy policy;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final Object summaryLock = new Object();

    public AlertService(AlertEventRepository eventRepository, NotificationPolicy policy, Clock appClock,
                        PlatformTransactionManager transactionManager) {
        this.eventRepository = eventRepository;
        this.policy = policy;
        this.clock = appClock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void evaluateNewObservation(TrackingEntity tracking, PriceObservationEntity observation,
                                       PriceObservationEntity previous, Instant generatedAt) {
        evaluateTarget(tracking, observation, generatedAt);
        if (tracking.isDropEnabled() && observation.getPrice() != null && previous != null
                && previous.getPrice() != null && observation.getPrice().compareTo(previous.getPrice()) < 0) {
            createPriceEvent(tracking, observation, previous, DROP, generatedAt);
        }
    }

    @Transactional
    public void evaluateTarget(TrackingEntity tracking, PriceObservationEntity observation, Instant generatedAt) {
        if (observation.getPrice() == null || tracking.getTargetPrice() == null) return;
        if (observation.getPrice().compareTo(tracking.getTargetPrice()) > 0) {
            tracking.setTargetArmed(true);
            return;
        }
        if (!tracking.isTargetArmed()) return;

        createPriceEvent(tracking, observation, null, TARGET, generatedAt);
        tracking.setTargetArmed(false);
    }

    @Transactional
    public void evaluateCurrentTarget(TrackingEntity tracking, BigDecimal price, Instant retrievedAt, Instant generatedAt) {
        if (price == null || tracking.getTargetPrice() == null) return;
        if (price.compareTo(tracking.getTargetPrice()) > 0) {
            tracking.setTargetArmed(true);
            return;
        }
        if (!tracking.isTargetArmed()) return;

        createPriceEvent(tracking, null, TARGET, price, retrievedAt, null, null, generatedAt);
        tracking.setTargetArmed(false);
    }

    public void processDue(Instant now) {
        synchronized (summaryLock) {
            transactionTemplate.executeWithoutResult(status -> processDueInTransaction(now));
        }
    }

    private void processDueInTransaction(Instant now) {
        var settings = policy.settings();
        ZonedDateTime localNow = now.atZone(NotificationPolicy.ZONE);
        LocalDate today = localNow.toLocalDate();
        if (policy.isQuiet(now)) return;
        Instant scheduledToday = summaryTime(today, settings.summaryTime());
        Instant cutoff = now.isBefore(scheduledToday)
                ? summaryTime(today.minusDays(1), settings.summaryTime()) : now;
        if (now.isBefore(cutoff)) return;
        if (eventRepository.existsBySummaryDateAndType(today, SUMMARY)) return;
        List<AlertEventEntity> pending = eventRepository
                .findByTypeInAndReadAtIsNullAndSummarizedAtIsNull(PRICE_ALERTS).stream()
                .filter(event -> !event.getCreatedAt().isAfter(cutoff))
                .filter(event -> isAvailable(event, now))
                .toList();
        if (pending.isEmpty()) return;

        eventRepository.save(new AlertEventEntity(today, pending.size(), now));
        pending.forEach(event -> event.markSummarized(now));
        eventRepository.flush();
    }

    private Instant summaryTime(LocalDate date, LocalTime summaryTime) {
        return policy.nextAllowed(date.atTime(summaryTime)
                .atZone(NotificationPolicy.ZONE).toInstant());
    }

    @Transactional
    public List<AlertEventView> events(Instant now) {
        return eventRepository.findTop100ByOrderByCreatedAtDescIdDesc().stream()
                .map(event -> toView(event, now))
                .toList();
    }

    @Transactional
    public void markRead(long eventId) {
        AlertEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Alert was not found."));
        if (event.getReadAt() == null) event.markRead(clock.instant());
    }

    @Transactional
    public void snooze(long eventId) {
        AlertEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Alert was not found."));
        event.snoozeUntil(clock.instant().plusSeconds(SNOOZE_HOURS * 60 * 60));
    }

    private void createPriceEvent(TrackingEntity tracking, PriceObservationEntity observation,
                                  PriceObservationEntity previous, String type, Instant generatedAt) {
        if (eventRepository.existsByTracking_IdAndObservationIdAndType(
                tracking.getId(), observation.getId(), type)) return;
        createPriceEvent(tracking, observation.getId(), type, observation.getPrice(), observation.getObservedAt(),
                previous == null ? null : previous.getPrice(),
                previous == null ? null : previous.getObservedAt(), generatedAt);
    }

    private void createPriceEvent(TrackingEntity tracking, Long observationId, String type, BigDecimal price,
                                  Instant observedAt, BigDecimal previousPrice, Instant previousObservedAt,
                                  Instant generatedAt) {
        eventRepository.save(new AlertEventEntity(tracking, observationId, type, price,
                observedAt, previousPrice, previousObservedAt,
                generatedAt, policy.nextAllowed(generatedAt)));
    }

    private boolean isAvailable(AlertEventEntity event, Instant now) {
        Instant base = event.getSnoozedUntil() != null && event.getSnoozedUntil().isAfter(event.getCreatedAt())
                ? event.getSnoozedUntil() : event.getCreatedAt();
        return !policy.isQuiet(now) && !now.isBefore(policy.nextAllowed(base));
    }

    private AlertEventView toView(AlertEventEntity event, Instant now) {
        TrackingEntity tracking = event.getTracking();
        String productCode = tracking == null ? null : tracking.getProductCode();
        String productName = tracking == null || tracking.getProduct().getName() == null
                ? productCode : tracking.getProduct().getName().getEn();
        String retailerCode = tracking == null ? null : tracking.getRetailerCode();
        Instant base = event.getSnoozedUntil() != null && event.getSnoozedUntil().isAfter(event.getCreatedAt())
                ? event.getSnoozedUntil() : event.getCreatedAt();
        Instant availableAt = policy.nextAllowed(base);
        if (policy.isQuiet(availableAt)) availableAt = policy.nextAllowed(availableAt);
        if (policy.isQuiet(now) && availableAt.isBefore(policy.nextAllowed(now))) {
            availableAt = policy.nextAllowed(now);
        }
        boolean available = !policy.isQuiet(now) && !now.isBefore(availableAt);
        String message = switch (event.getType()) {
            case TARGET -> "Target reached for " + productName + " at " + retailerCode + ".";
            case DROP -> "Price dropped for " + productName + " at " + retailerCode + ".";
            case SUMMARY -> "You have " + event.getSummaryCount() + " new price alert"
                    + (event.getSummaryCount() == 1 ? "" : "s") + " since your last summary.";
            default -> "Price update.";
        };
        return new AlertEventView(event.getId(), event.getType(), message, productCode, productName,
                retailerCode, event.getPrice(), event.getObservedAt(), event.getPreviousPrice(),
                event.getPreviousObservedAt(), event.getCreatedAt(), availableAt, event.getSnoozedUntil(),
                event.getReadAt() != null, available, event.getSummaryCount(), event.getSummaryDate());
    }
}
