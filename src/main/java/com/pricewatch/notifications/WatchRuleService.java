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
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.tracking.repository.SavedProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WatchRuleService {
    private final WatchRuleRepository rules;
    private final ProductTargetEventRepository events;
    private final ProductRepository products;
    private final RetailerListingRepository listings;
    private final SavedProductRepository wishlist;
    private final Clock clock;
    private final ShoppingService shopping;

    public WatchRuleService(WatchRuleRepository rules, ProductTargetEventRepository events, ProductRepository products,
                            RetailerListingRepository listings, SavedProductRepository wishlist, Clock clock,
                            ShoppingService shopping) {
        this.rules = rules;
        this.events = events;
        this.products = products;
        this.listings = listings;
        this.wishlist = wishlist;
        this.clock = clock;
        this.shopping = shopping;
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
            if (existing.getCartQuantity() != null) throw new IllegalArgumentException("Edit this time reminder in its own form.");
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
        save(code, id, target, startAt, repeat, WatchPriceKind.TARGET);
    }

    @Transactional
    public void save(String code, Long id, BigDecimal target, LocalDateTime startAt, String repeat, WatchPriceKind kind) {
        BigDecimal price = target == null ? null : kind.validate(target);
        if (price == null && startAt == null) {
            throw new IllegalArgumentException("Set a price target or a time condition.");
        }
        WatchRepeat frequency = price != null && startAt == null && (repeat == null || repeat.isBlank())
                ? WatchRepeat.DAILY : parseRepeat(repeat);
        if (id == null) {
            requireProduct(code);
            if (price != null) requireWishlist(code);
            var rule = new WatchRuleEntity(code, price, startAt, frequency);
            rule.updateSchedule(price, startAt, frequency, kind);
            rules.save(rule);
            return;
        }

        var existing = requireRule(code, id);
        if (existing.getCartQuantity() != null) throw new IllegalArgumentException("Edit this time reminder in its own form.");
        boolean wishlisted = wishlist.existsById(code);
        if (price != null && existing.getTarget() == null && !wishlisted) requireWishlist(code);
        existing.updateSchedule(price, startAt, frequency, kind);
    }

    @Transactional
    public void setEnabled(String code, long id, boolean enabled) {
        requireRule(code, id).setEnabled(enabled);
    }

    @Transactional
    public void delete(String code, long id) {
        var rule = requireRule(code, id);
        if (rule.getCartQuantity() != null) {
            events.findByRuleIdAndScheduledDueAt(id, rule.getNextDueAt()).ifPresent(event -> {
                event.markRead(clock.instant());
                event.snoozeUntil(null);
            });
        }
        rules.delete(rule);
    }

    @Transactional
    public void reset() {
        rules.deleteAllInBatch();
    }

    @Transactional
    public void updateMigratedTimeRule(String code, long id, int intervalDays, Integer quantity, String retailer) {
        var rule = requireRule(code, id);
        if (rule.getCartQuantity() == null) throw new IllegalArgumentException("That reminder no longer exists.");
        int packs = quantity == null ? rule.getCartQuantity() : quantity;
        String choice = quantity == null ? rule.getCartRetailer()
                : retailer == null || retailer.isBlank() ? null : retailer.strip();
        if (intervalDays < 1 || packs < 1 || packs > 999) {
            throw new IllegalArgumentException("Use a positive interval and a quantity from 1 to 999.");
        }
        if (choice != null && !choice.equals(rule.getCartRetailer())
                && products.findWithListingsByCode(code).stream().flatMap(product -> product.getListings().stream())
                .noneMatch(listing -> choice.equals(listing.getRetailerCode()))) {
            throw new IllegalArgumentException("Choose a listed retailer for this product, or use Automatic lowest.");
        }
        Instant now = clock.instant();
        events.findByRuleIdAndScheduledDueAt(id, rule.getNextDueAt())
                .filter(event -> event.getReadAt() == null).ifPresent(event -> {
                    if (Objects.equals(rule.getIntervalDays(), intervalDays)) {
                        event.reviseCartDefaults(packs, choice);
                    } else {
                        event.markRead(now);
                        event.snoozeUntil(null);
                    }
                });
        rule.reviseCartReminder(intervalDays, packs, choice, now);
    }

    @Transactional
    public boolean dismissCartReminder(long eventId) {
        var current = currentCartReminder(eventId);
        if (current.isEmpty()) return false;
        Instant now = clock.instant();
        current.get().event().markRead(now);
        current.get().event().snoozeUntil(null);
        current.get().rule().advance(now);
        return true;
    }

    @Transactional
    public boolean snoozeCartReminder(long eventId) {
        var current = currentCartReminder(eventId);
        if (current.isEmpty()) return false;
        var rule = current.get().rule();
        Instant now = clock.instant();
        if (!rule.isDue(now)) throw missingReminder();
        Instant until = now.plus(1, ChronoUnit.DAYS);
        rule.snoozeReminder(until);
        current.get().event().snoozeUntil(until);
        return true;
    }

    @Transactional
    public void addReminderToCart(long eventId) {
        var rule = currentCartReminder(eventId).orElseThrow(WatchRuleService::missingReminder).rule();
        if (!rule.isDue(clock.instant())) throw missingReminder();
        shopping.addFromReminder(rule.getProductCode(), rule.getCartQuantity(), rule.getCartRetailer());
    }

    private Optional<CartReminder> currentCartReminder(long eventId) {
        var reference = events.findReminderReference(eventId);
        if (reference.isEmpty()) return Optional.empty();
        var ruleId = reference.get().getRuleId();
        if (ruleId == null) throw missingReminder();
        var rule = rules.findForUpdate(ruleId, reference.get().getProductCode())
                .orElseThrow(WatchRuleService::missingReminder);
        var event = events.findLocked(eventId).orElseThrow(WatchRuleService::missingReminder);
        if (rule.getCartQuantity() == null || !rule.isEnabled() || event.getReadAt() != null
                || !Objects.equals(rule.getNextDueAt(), event.getScheduledDueAt())) {
            throw missingReminder();
        }
        return Optional.of(new CartReminder(rule, event));
    }

    private record CartReminder(WatchRuleEntity rule, ProductTargetEventEntity event) {}

    private static IllegalArgumentException missingReminder() {
        return new IllegalArgumentException("Alert was not found.");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<String, Map<String, BigDecimal>> previousDropPrices() {
        var codes = rules.priceBaselineProductCodes(List.of(WatchPriceKind.DROP_AMOUNT, WatchPriceKind.DROP_PERCENT));
        if (codes.isEmpty()) return Map.of();
        return listings.findAcceptedLivePrices(codes).stream().collect(Collectors.groupingBy(
                LiveListing::getProductCode, Collectors.toMap(LiveListing::getRetailerCode, LiveListing::getPrice)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluateLive(ImportedCatalog catalog, Instant retrievedAt,
                             Map<String, Map<String, BigDecimal>> previousPrices) {
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
            var qualifying = qualifyingPrices(rule, known, previousPrices.getOrDefault(rule.getProductCode(), Map.of()));
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
                if (rule.getCartQuantity() == null) {
                    recordEvent(rule, null, now, List.of());
                    rule.advance(now);
                } else if (events.findByRuleIdAndScheduledDueAt(rule.getId(), rule.getNextDueAt()).isEmpty()) {
                    var occurrence = new ProductTargetEventEntity(rule.getProductCode(), null, null, now,
                            List.of(), rule.getId(), rule.getNextDueAt());
                    occurrence.reviseCartDefaults(rule.getCartQuantity(), rule.getCartRetailer());
                    events.save(occurrence);
                }
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
            if (rule.getStartAt() != null) {
                rule.waitForLive();
                continue;
            }
            var currentPrices = byCode.getOrDefault(rule.getProductCode(), List.of());
            if (currentPrices.isEmpty()) {
                rule.waitForLive();
                continue;
            }
            var known = currentPrices.stream()
                    .map(listing -> new ProductTargetRetailerPrice(listing.getRetailerCode(), listing.getPrice()))
                    .toList();
            evaluateDue(rule, currentPrices.getFirst().getRetrievedAt(), now, qualifyingPrices(rule, known, Map.of()));
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
        var event = new ProductTargetEventEntity(rule.getProductCode(), rule.getTarget(), retrievedAt, now,
                qualifying, rule.getId(), rule.getNextDueAt());
        event.setPriceKind(rule.getPriceKind());
        events.save(event);
    }

    private static List<ProductTargetRetailerPrice> qualifyingPrices(WatchRuleEntity rule,
                                                                     List<ProductTargetRetailerPrice> known,
                                                                     Map<String, BigDecimal> previousPrices) {
        var kind = rule.getPriceKind();
        return known.stream()
                .map(price -> kind == WatchPriceKind.TARGET ? price : new ProductTargetRetailerPrice(
                        price.retailerCode(), price.price(), previousPrices.get(price.retailerCode())))
                .filter(price -> kind.matches(rule.getTarget(), price.price(), price.previousPrice()))
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
                rule.getRepeat() == null ? null : rule.getRepeat().name(), rule.getStartAt(), rule.isCompleted(),
                rule.getCartQuantity(), rule.getCartRetailer(), rule.getSnoozedUntil(), rule.getPriceKind());
    }
}
