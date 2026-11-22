package com.pricewatch.notifications;

import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AlertService {
    public static final String TARGET = "TARGET";
    public static final String DROP = "DROP";
    private static final String SUMMARY = "SUMMARY";
    private static final Duration SNOOZE_DURATION = Duration.ofHours(24);

    private final AlertEventRepository eventRepository;
    private final Clock clock;

    public AlertService(AlertEventRepository eventRepository, Clock appClock) {
        this.eventRepository = eventRepository;
        this.clock = appClock;
    }

    @Transactional
    public void evaluateNewObservation(TrackingEntity tracking, PriceObservationEntity observation,
                                       PriceObservationEntity previous, Instant generatedAt) {
        if (!tracking.isAlertsEnabled()) return;
        evaluateTarget(tracking, observation, generatedAt);
        if (tracking.isDropEnabled() && observation.getPrice() != null && previous != null
                && previous.getPrice() != null && observation.getPrice().compareTo(previous.getPrice()) < 0) {
            createPriceEvent(tracking, observation, previous, DROP, generatedAt);
        }
    }

    @Transactional
    public void evaluateTarget(TrackingEntity tracking, PriceObservationEntity observation, Instant generatedAt) {
        if (!tracking.isAlertsEnabled() || observation.getPrice() == null || tracking.getTargetPrice() == null) return;
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
        if (!tracking.isAlertsEnabled() || price == null || tracking.getTargetPrice() == null) return;
        if (price.compareTo(tracking.getTargetPrice()) > 0) {
            tracking.setTargetArmed(true);
            return;
        }
        if (!tracking.isTargetArmed()) return;

        createPriceEvent(tracking, null, TARGET, price, retrievedAt, null, null, generatedAt);
        tracking.setTargetArmed(false);
    }

    @Transactional(readOnly = true)
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
        event.snoozeUntil(clock.instant().plus(SNOOZE_DURATION));
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
                generatedAt, generatedAt));
    }

    private AlertEventView toView(AlertEventEntity event, Instant now) {
        TrackingEntity tracking = event.getTracking();
        String productCode = tracking == null ? null : tracking.getProductCode();
        String productName = tracking == null || tracking.getProduct().getName() == null
                ? productCode : tracking.getProduct().getName().getEn();
        String retailerCode = tracking == null ? null : tracking.getRetailerCode();
        // Older records may still contain a quiet-hours delay. Only an explicit snooze affects availability.
        Instant availableAt = event.getSnoozedUntil() != null && event.getSnoozedUntil().isAfter(event.getCreatedAt())
                ? event.getSnoozedUntil() : event.getCreatedAt();
        boolean available = !now.isBefore(availableAt);
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
