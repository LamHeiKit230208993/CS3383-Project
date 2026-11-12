package com.pricewatch.alerts;

import com.pricewatch.alerts.persistence.AlertEventEntity;
import com.pricewatch.alerts.persistence.AlertRuleEntity;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {
    private static final long SNOOZE_HOURS = 8;

    private final AlertRuleRepository ruleRepository;
    private final AlertEventRepository eventRepository;
    private final PriceObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final Object mutationLock = new Object();
    private final Clock clock;

    public AlertService(AlertRuleRepository ruleRepository,
                        AlertEventRepository eventRepository,
                        PriceObservationRepository observationRepository,
                        ProductRepository productRepository,
                        PlatformTransactionManager transactionManager,
                        Clock clock) {
        this.ruleRepository = ruleRepository;
        this.eventRepository = eventRepository;
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public void createRule(String productCode, String retailerCode, AlertRuleEntity.Type type,
                           BigDecimal targetAmount) {
        if (type == AlertRuleEntity.Type.TARGET && (targetAmount == null || targetAmount.signum() <= 0)) {
            throw new IllegalArgumentException("Enter a target price greater than zero.");
        }
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> {
                AlertRuleEntity rule = new AlertRuleEntity(productCode, retailerCode, type,
                        type == AlertRuleEntity.Type.TARGET ? targetAmount : null);
                AlertRuleEntity saved = ruleRepository.save(rule);
                evaluateRule(saved);
            });
        }
    }

    public void deleteRule(Long ruleId) {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> ruleRepository.deleteById(ruleId));
        }
    }

    public void acknowledge(Long eventId) {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status ->
                    eventRepository.findById(eventId).ifPresent(event -> {
                        event.setAcknowledged(true);
                        eventRepository.save(event);
                    }));
        }
    }

    public void snooze(Long eventId) {
        Instant until = clock.instant().plusSeconds(SNOOZE_HOURS * 3600);
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status ->
                    eventRepository.findById(eventId).ifPresent(event -> {
                        event.setSnoozedUntil(until);
                        eventRepository.save(event);
                    }));
        }
    }

    /** Re-evaluate every enabled rule. Safe to call on page load; no duplicates. */
    public void evaluateAll() {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status ->
                    ruleRepository.findAllByEnabledTrue().forEach(this::evaluateRule));
        }
    }

    private void evaluateRule(AlertRuleEntity rule) {
        Optional<PriceObservationEntity> latest = observationRepository
                .findAllByProductCodeAndRetailerCodeOrderByRetrievedAtAscIdAsc(
                        rule.getProductCode(), rule.getRetailerCode())
                .stream().reduce((first, second) -> second);
        if (latest.isEmpty() || latest.get().getPrice() == null) {
            return;
        }
        PriceObservationEntity current = latest.get();
        if (current.getId().equals(rule.getLastFiredObservationId())) {
            return;
        }
        boolean fires = switch (rule.getType()) {
            case TARGET -> current.getPrice().compareTo(rule.getTargetAmount()) <= 0;
            case DROP -> firesOnDrop(rule);
        };
        if (!fires || eventRepository.existsByRuleIdAndObservationId(rule.getId(), current.getId())) {
            return;
        }
        eventRepository.save(new AlertEventEntity(rule.getId(), current.getId(), rule.getProductCode(),
                rule.getRetailerCode(), rule.getType().name(), current.getPrice(), clock.instant()));
        rule.setLastFiredObservationId(current.getId());
        ruleRepository.save(rule);
    }

    /** A drop compares the latest price with the most recent distinct prior price. */
    private boolean firesOnDrop(AlertRuleEntity rule) {
        List<PriceObservationEntity> observations = observationRepository
                .findAllByProductCodeAndRetailerCodeOrderByRetrievedAtAscIdAsc(
                        rule.getProductCode(), rule.getRetailerCode());
        if (observations.size() < 2) {
            return false;
        }
        BigDecimal latestPrice = observations.getLast().getPrice();
        for (int index = observations.size() - 2; index >= 0; index--) {
            BigDecimal prior = observations.get(index).getPrice();
            if (prior != null && prior.compareTo(latestPrice) != 0) {
                return latestPrice.compareTo(prior) < 0;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public AlertPageView view() {
        Instant now = clock.instant();
        boolean quiet = NotificationPolicy.isQuietHours(clock);
        List<AlertPageView.RuleView> rules = ruleRepository.findAllByOrderByIdAsc().stream()
                .map(rule -> new AlertPageView.RuleView(rule.getId(), rule.getProductCode(),
                        productName(rule.getProductCode()), rule.getRetailerCode(),
                        rule.getType().name(), rule.getTargetAmount(), rule.isEnabled()))
                .toList();

        List<AlertPageView.EventView> events = new ArrayList<>();
        for (AlertEventEntity event : eventRepository.findAllByOrderByFiredAtDescIdDesc()) {
            boolean snoozedNow = event.getSnoozedUntil() != null && event.getSnoozedUntil().isAfter(now);
            boolean visible = !quiet && !snoozedNow && !event.isAcknowledged();
            events.add(toEventView(event, visible));
        }

        // Morning summary: events since the latest 08:00 boundary.
        Instant since = NotificationPolicy.latestSummaryTime(clock).toInstant();
        List<AlertPageView.EventView> summaryEvents = eventRepository
                .findAllByFiredAtAfterOrderByFiredAtDescIdDesc(since).stream()
                .map(event -> toEventView(event, false))
                .toList();
        return new AlertPageView(quiet, rules, events,
                new AlertPageView.SummaryView(since, summaryEvents));
    }

    private AlertPageView.EventView toEventView(AlertEventEntity event, boolean visible) {
        return new AlertPageView.EventView(event.getId(), event.getProductCode(),
                productName(event.getProductCode()), event.getRetailerCode(), event.getRuleType(),
                event.getPrice(), event.getFiredAt(), event.isAcknowledged(),
                event.getSnoozedUntil(), visible);
    }

    private String productName(String code) {
        return productRepository.findById(code)
                .map(ProductEntity::getName)
                .map(name -> name == null ? code : name.getEn())
                .orElse(code);
    }
}
