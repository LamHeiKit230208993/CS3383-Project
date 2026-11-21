package com.pricewatch.notifications;

import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository.LiveListing;
import com.pricewatch.notifications.persistence.ProductTargetEventEntity;
import com.pricewatch.notifications.persistence.ProductTargetRetailerPrice;
import com.pricewatch.notifications.persistence.WatchRuleEntity;
import com.pricewatch.notifications.repository.ProductTargetEventRepository;
import com.pricewatch.notifications.repository.WatchRuleRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchRuleService {
    private final WatchRuleRepository rules;
    private final ProductTargetEventRepository events;
    private final ProductRepository products;
    private final RetailerListingRepository listings;
    private final SavedProductRepository wishlist;
    private final Clock clock;

    public WatchRuleService(WatchRuleRepository rules, ProductTargetEventRepository events, ProductRepository products,
                            RetailerListingRepository listings, SavedProductRepository wishlist, Clock clock) {
        this.rules = rules;
        this.events = events;
        this.products = products;
        this.listings = listings;
        this.wishlist = wishlist;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WatchRuleView> rules(String code) {
        return rules.findByProductCodeOrderById(code).stream().map(WatchRuleService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<WatchRuleView> allRules() {
        return rules.findAllByOrderById().stream().map(WatchRuleService::view).toList();
    }

    @Transactional
    public void save(String code, Long id, BigDecimal target, Integer intervalDays) {
        BigDecimal price = ProductTargetService.normalizePrice(target);
        if (intervalDays != null && (intervalDays < 1 || intervalDays > 365)) {
            throw new IllegalArgumentException("Use an interval from 1 to 365 days.");
        }
        Instant now = clock.instant();
        if (id != null) {
            var existing = requireRule(code, id);
            if (existing.getRepeat() != null || (existing.getIntervalDays() == null) != (intervalDays == null)) {
                throw new IllegalArgumentException("Create a new rule to use a different rule type.");
            }
            if (existing.isDue(now) && existing.isEnabled() && !existing.isWaitingForLive()
                    && existing.getIntervalDays().equals(intervalDays) && wishlist.existsById(code)) {
                evaluateAcceptedPrices(List.of(existing), now);
            }
            existing.update(price, intervalDays, now);
            return;
        }
        requireProduct(code);
        requireWishlist(code);
        rules.save(new WatchRuleEntity(code, price, intervalDays, now));
    }

    @Transactional
    public void save(String code, Long id, BigDecimal target, LocalDateTime startAt, String repeat) {
        BigDecimal price = target == null ? null : ProductTargetService.normalizePrice(target);
        if (price == null && startAt == null) {
            throw new IllegalArgumentException("Set a price target or a time condition.");
        }
        WatchRepeat frequency = parseRepeat(repeat);
        if (id == null) {
            requireProduct(code);
            if (price != null) requireWishlist(code);
            rules.save(new WatchRuleEntity(code, price, startAt, frequency));
            return;
        }

        var existing = requireRule(code, id);
        boolean wishlisted = wishlist.existsById(code);
        if (price != null && existing.getTarget() == null && !wishlisted) requireWishlist(code);
        Instant now = clock.instant();
        if (price != null && existing.getTarget() != null && existing.isEnabled() && existing.isDue(now)
                && !existing.isWaitingForLive() && existing.matchesSchedule(startAt, frequency) && wishlisted) {
            evaluateAcceptedPrices(List.of(existing), now);
        }
        existing.updateSchedule(price, startAt, frequency);
    }

    @Transactional
    public void setEnabled(String code, long id, boolean enabled) {
        requireRule(code, id).setEnabled(enabled);
    }

    @Transactional
    public void delete(String code, long id) {
        rules.delete(requireRule(code, id));
    }

    @Transactional
    public void reset() {
        rules.deleteAllInBatch();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluateLive(ImportedCatalog catalog, Instant retrievedAt) {
        var enabled = rules.findWishlistedForUpdate();
        if (enabled.isEmpty()) return;
        var byCode = catalog.products().stream().collect(Collectors.toMap(
                ImportedCatalog.ImportedProduct::code, ImportedCatalog.ImportedProduct::listings));
        Instant now = clock.instant();
        for (var rule : enabled) {
            if (!rule.isEnabled() || rule.getTarget() == null) continue;
            var known = byCode.getOrDefault(rule.getProductCode(), List.of()).stream()
                    .filter(listing -> listing.price() != null)
                    .map(listing -> new ProductTargetRetailerPrice(listing.retailerCode(), listing.price()))
                    .toList();
            var qualifying = qualifyingPrices(rule, known);
            if (rule.getIntervalDays() != null || rule.getStartAt() != null) {
                if (rule.isDue(now)) evaluateDue(rule, retrievedAt, now, qualifying);
            } else if (rule.getRepeat() == null) {
                if (!known.isEmpty() && rule.evaluatePrice(retrievedAt, !qualifying.isEmpty())) {
                    recordEvent(rule, retrievedAt, now, qualifying);
                }
            } else if (!qualifying.isEmpty() && rule.canFirePrice(retrievedAt)) {
                recordEvent(rule, retrievedAt, now, qualifying);
                rule.recordPrice(retrievedAt, now);
            }
        }
    }

    @Transactional
    public void checkDue() {
        Instant now = clock.instant();
        var due = rules.findNewlyDueForUpdate(now).stream()
                .filter(rule -> rule.isEnabled() && rule.isDue(now) && !rule.isWaitingForLive()).toList();
        for (var rule : due) {
            if (rule.getTarget() == null) {
                recordEvent(rule, null, now, List.of());
                rule.advance(now);
            }
        }
        evaluateAcceptedPrices(due.stream().filter(rule -> rule.getTarget() != null).toList(), now);
    }

    private void evaluateAcceptedPrices(List<WatchRuleEntity> due, Instant now) {
        if (due.isEmpty()) return;
        var productCodes = due.stream().map(WatchRuleEntity::getProductCode).distinct().toList();
        // Read listings and import provenance in one statement. A refresh replaces both together.
        var byCode = listings.findAcceptedLivePrices(productCodes).stream()
                .collect(Collectors.groupingBy(LiveListing::getProductCode));
        for (var rule : due) {
            if (!rule.isDue(now) || rule.isWaitingForLive()) continue;
            var currentPrices = byCode.getOrDefault(rule.getProductCode(), List.of());
            if (currentPrices.isEmpty()) {
                rule.waitForLive();
                continue;
            }
            var known = currentPrices.stream()
                    .map(listing -> new ProductTargetRetailerPrice(listing.getRetailerCode(), listing.getPrice()))
                    .toList();
            evaluateDue(rule, currentPrices.getFirst().getRetrievedAt(), now, qualifyingPrices(rule, known));
        }
    }

    private void evaluateDue(WatchRuleEntity rule, Instant retrievedAt, Instant now,
                             List<ProductTargetRetailerPrice> qualifying) {
        if (qualifying.isEmpty()) {
            rule.waitForLive();
        } else {
            recordEvent(rule, retrievedAt, now, qualifying);
            rule.advance(now);
        }
    }

    private void recordEvent(WatchRuleEntity rule, Instant retrievedAt, Instant now,
                             List<ProductTargetRetailerPrice> qualifying) {
        events.save(new ProductTargetEventEntity(rule.getProductCode(), rule.getTarget(), retrievedAt, now,
                qualifying, rule.getId(), rule.getNextDueAt()));
    }

    private static List<ProductTargetRetailerPrice> qualifyingPrices(WatchRuleEntity rule,
                                                                     List<ProductTargetRetailerPrice> known) {
        return known.stream().filter(price -> price.price().compareTo(rule.getTarget()) <= 0)
                .sorted(Comparator.comparing(ProductTargetRetailerPrice::retailerCode)).toList();
    }

    private WatchRuleEntity requireRule(String code, long id) {
        return rules.findForUpdate(id, code)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule was not found."));
    }

    private void requireProduct(String code) {
        products.findLockedByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
    }

    private void requireWishlist(String code) {
        if (!wishlist.existsById(code)) {
            throw new IllegalArgumentException("Save this product to Wishlist before setting a price target.");
        }
    }

    private static WatchRepeat parseRepeat(String repeat) {
        try {
            return WatchRepeat.valueOf(repeat == null ? "" : repeat);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose a valid repeat schedule.");
        }
    }

    private static WatchRuleView view(WatchRuleEntity rule) {
        return new WatchRuleView(rule.getId(), rule.getProductCode(), rule.getTarget(), rule.getIntervalDays(),
                rule.getNextDueAt(), rule.isArmed(), rule.isWaitingForLive(), rule.isEnabled(),
                rule.getRepeat() == null ? null : rule.getRepeat().name(), rule.getStartAt(), rule.isCompleted());
    }
}
