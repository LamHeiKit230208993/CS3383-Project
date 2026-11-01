package com.pricewatch.alerts;

import com.pricewatch.alerts.persistence.AlertEventEntity;
import com.pricewatch.alerts.persistence.AlertRuleEntity;
import com.pricewatch.alerts.persistence.AlertRuleEntity.Type;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * J06: in-app target and price-drop alerts. Target uses one current known price
 * (at or below, D04). Drop needs the latest prior known observation for the same
 * product/retailer. Unknown observations never fire (D03). Quiet hours 22:00–07:00
 * hold events unshown; snooze 24h hides an event (D05). A closed app never delivers.
 */
@Service
public class AlertService {
    private static final LocalTime QUIET_START = LocalTime.of(22, 0);
    private static final LocalTime QUIET_END = LocalTime.of(7, 0);
    private static final Duration SNOOZE = Duration.ofHours(24);

    private final AlertRuleRepository rules;
    private final AlertEventRepository events;
    private final PriceObservationRepository observations;
    private final Clock clock;

    public AlertService(AlertRuleRepository rules, AlertEventRepository events,
                        PriceObservationRepository observations, Clock clock) {
        this.rules = rules;
        this.events = events;
        this.observations = observations;
        this.clock = clock;
    }

    @Transactional
    public AlertRuleEntity createRule(String productCode, String retailerCode, Type type, BigDecimal targetAmount) {
        if (type == Type.TARGET && targetAmount == null) {
            throw new IllegalArgumentException("A target alert needs a target amount.");
        }
        return rules.save(new AlertRuleEntity(productCode, retailerCode, type, targetAmount));
    }

    @Transactional
    public void disableRule(Long ruleId) {
        rules.findById(ruleId).ifPresent(AlertRuleEntity::disable);
    }

    /**
     * Evaluate enabled rules for a tracked pair after new observations were
     * recorded. Uses the latest known observation as the current price and, for a
     * drop, the latest prior known observation. Returns the number of new events.
     */
    @Transactional
    public int evaluate(String productCode, String retailerCode) {
        List<PriceObservationEntity> history =
                observations.findAllByProductCodeAndRetailerCodeOrderByRetrievedAtAscIdAsc(productCode, retailerCode);
        Optional<PriceObservationEntity> currentKnown = lastKnown(history);
        if (currentKnown.isEmpty()) {
            return 0;
        }
        PriceObservationEntity current = currentKnown.get();
        int created = 0;
        for (AlertRuleEntity rule : rules.findAllByProductCodeAndRetailerCodeAndEnabledTrue(productCode, retailerCode)) {
            BigDecimal eventPrice = switch (rule.getType()) {
                case TARGET -> matchesTarget(rule, current) ? current.getPrice() : null;
                case DROP -> dropPrice(history, current);
            };
            if (eventPrice != null && !events.existsByProductCodeAndRetailerCodeAndTypeAndPrice(
                    productCode, retailerCode, rule.getType(), eventPrice)) {
                BigDecimal compared = rule.getType() == Type.DROP ? priorKnownPrice(history, current).orElse(null) : null;
                events.save(new AlertEventEntity(productCode, retailerCode, rule.getType(), eventPrice, compared, clock.instant()));
                created++;
            }
        }
        return created;
    }

    private static boolean matchesTarget(AlertRuleEntity rule, PriceObservationEntity current) {
        return current.getPrice().compareTo(rule.getTargetAmount()) <= 0;
    }

    private static BigDecimal dropPrice(List<PriceObservationEntity> history, PriceObservationEntity current) {
        Optional<BigDecimal> prior = priorKnownPrice(history, current);
        return prior.filter(value -> current.getPrice().compareTo(value) < 0).map(value -> current.getPrice()).orElse(null);
    }

    private static Optional<BigDecimal> priorKnownPrice(List<PriceObservationEntity> history, PriceObservationEntity current) {
        BigDecimal prior = null;
        for (PriceObservationEntity obs : history) {
            if (obs.getId().equals(current.getId())) {
                break;
            }
            if (obs.isKnown()) {
                prior = obs.getPrice();
            }
        }
        return Optional.ofNullable(prior);
    }

    private static Optional<PriceObservationEntity> lastKnown(List<PriceObservationEntity> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isKnown()) {
                return Optional.of(history.get(i));
            }
        }
        return Optional.empty();
    }

    // --- In-app viewing, quiet hours, snooze, summary ---

    @Transactional(readOnly = true)
    public List<AlertEventEntity> allEvents() {
        return events.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<AlertEventEntity> heldEvents() {
        return events.findAllByVisibleFalseOrderByCreatedAtAscIdAsc();
    }

    public boolean inQuietHours() {
        LocalTime now = ZonedDateTime.now(clock).toLocalTime();
        return !now.isBefore(QUIET_START) || now.isBefore(QUIET_END);
    }

    /** Show held events now if outside quiet hours and not snoozed. */
    @Transactional
    public int showHeldNow() {
        if (inQuietHours()) {
            return 0;
        }
        Instant now = clock.instant();
        int shown = 0;
        for (AlertEventEntity event : events.findAllByVisibleFalseOrderByCreatedAtAscIdAsc()) {
            boolean snoozed = event.getSnoozeUntil() != null && event.getSnoozeUntil().isAfter(now);
            if (!snoozed) {
                event.markVisible();
                shown++;
            }
        }
        return shown;
    }

    @Transactional
    public void snooze(Long eventId) {
        events.findById(eventId).ifPresent(event -> event.snooze(clock.instant().plus(SNOOZE)));
    }

    /**
     * Morning summary: visible events from the last 24 hours, so alerts held
     * overnight during quiet hours appear in the morning summary (D05).
     */
    @Transactional(readOnly = true)
    public List<AlertEventEntity> morningSummary() {
        Instant since = clock.instant().minus(Duration.ofHours(24));
        return events.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(AlertEventEntity::isVisible)
                .filter(event -> !event.getCreatedAt().isBefore(since))
                .toList();
    }
}
