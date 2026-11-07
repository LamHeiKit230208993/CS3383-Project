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
    public AlertRuleEntity createRule(String productCode, Type type, BigDecimal targetAmount) {
        if (type == Type.TARGET && targetAmount == null) {
            throw new IllegalArgumentException("A target alert needs a target amount.");
        }
        return rules.save(new AlertRuleEntity(productCode, type, targetAmount));
    }

    @Transactional
    public void disableRule(Long ruleId) {
        rules.findById(ruleId).ifPresent(AlertRuleEntity::disable);
    }

    @Transactional
    public AlertRuleEntity updateRule(Long ruleId, Type type, BigDecimal targetAmount) {
        if (type == Type.TARGET && targetAmount == null) {
            throw new IllegalArgumentException("A target alert needs a target amount.");
        }
        AlertRuleEntity rule = rules.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("That alert rule no longer exists."));
        rule.update(type, targetAmount);
        return rule;
    }

    @Transactional(readOnly = true)
    public List<AlertRuleEntity> rulesForProduct(String productCode) {
        return rules.findAllByProductCodeAndEnabledTrue(productCode);
    }

    /**
     * Evaluate enabled rules for a product after new observations were recorded.
     * A TARGET rule fires when the lowest current known price across the product's
     * tracked retailers is at or below the target; the event names that retailer.
     * A DROP rule fires per retailer when its new known price is below its prior
     * known price. Returns the number of new events.
     */
    @Transactional
    public int evaluate(String productCode) {
        List<AlertRuleEntity> productRules = rules.findAllByProductCodeAndEnabledTrue(productCode);
        if (productRules.isEmpty()) {
            return 0;
        }
        List<PriceObservationEntity> history = observations.findAllByProductCodeOrderByRetrievedAtAscIdAsc(productCode);
        java.util.Map<String, List<PriceObservationEntity>> byRetailer = history.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PriceObservationEntity::getRetailerCode, java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        int created = 0;
        for (AlertRuleEntity rule : productRules) {
            created += switch (rule.getType()) {
                case TARGET -> evaluateTarget(productCode, rule, byRetailer);
                case DROP -> evaluateDrops(productCode, rule, byRetailer);
            };
        }
        return created;
    }

    private int evaluateTarget(String productCode, AlertRuleEntity rule,
                               java.util.Map<String, List<PriceObservationEntity>> byRetailer) {
        String bestRetailer = null;
        PriceObservationEntity best = null;
        for (var entry : byRetailer.entrySet()) {
            Optional<PriceObservationEntity> current = lastKnown(entry.getValue());
            if (current.isEmpty()) continue;
            if (best == null || current.get().getPrice().compareTo(best.getPrice()) < 0) {
                best = current.get();
                bestRetailer = entry.getKey();
            }
        }
        if (best == null || !matchesTarget(rule, best)) {
            return 0;
        }
        if (events.existsByProductCodeAndRetailerCodeAndTypeAndPrice(
                productCode, bestRetailer, Type.TARGET, best.getPrice())) {
            return 0;
        }
        events.save(new AlertEventEntity(productCode, bestRetailer, Type.TARGET, best.getPrice(), null, clock.instant()));
        return 1;
    }

    private int evaluateDrops(String productCode, AlertRuleEntity rule,
                              java.util.Map<String, List<PriceObservationEntity>> byRetailer) {
        int created = 0;
        for (var entry : byRetailer.entrySet()) {
            String retailerCode = entry.getKey();
            List<PriceObservationEntity> retailerHistory = entry.getValue();
            Optional<PriceObservationEntity> currentKnown = lastKnown(retailerHistory);
            if (currentKnown.isEmpty()) continue;
            PriceObservationEntity current = currentKnown.get();
            BigDecimal eventPrice = dropPrice(retailerHistory, current);
            if (eventPrice == null || events.existsByProductCodeAndRetailerCodeAndTypeAndPrice(
                    productCode, retailerCode, Type.DROP, eventPrice)) {
                continue;
            }
            BigDecimal compared = priorKnownPrice(retailerHistory, current).orElse(null);
            events.save(new AlertEventEntity(productCode, retailerCode, Type.DROP, eventPrice, compared, clock.instant()));
            created++;
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

    public record AlertEventView(AlertEventEntity event, AlertRuleEntity rule) {}

    @Transactional(readOnly = true)
    public List<AlertEventView> eventViews() {
        return events.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(event -> new AlertEventView(event, rules
                        .findFirstByProductCodeAndTypeAndEnabledTrue(
                                event.getProductCode(), event.getType())
                        .orElse(null)))
                .toList();
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
