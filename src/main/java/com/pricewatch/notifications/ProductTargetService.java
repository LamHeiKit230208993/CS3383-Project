package com.pricewatch.notifications;

import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.persistence.ProductTargetEventEntity;
import com.pricewatch.notifications.persistence.ProductTargetRetailerPrice;
import com.pricewatch.notifications.persistence.ProductTargetRuleEntity;
import com.pricewatch.notifications.repository.ProductTargetEventRepository;
import com.pricewatch.notifications.repository.ProductTargetRuleRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class ProductTargetService {
    private static final Duration SNOOZE = Duration.ofHours(24);
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999999999999.9999");
    private static final String INVALID_PRICE = "Use a price from 0 to 999999999999999.9999.";

    private final ProductRepository products;
    private final SavedProductRepository wishlist;
    private final ProductTargetRuleRepository rules;
    private final ProductTargetEventRepository events;
    private final Clock clock;

    public ProductTargetService(ProductRepository products, SavedProductRepository wishlist,
                                ProductTargetRuleRepository rules, ProductTargetEventRepository events, Clock clock) {
        this.products = products;
        this.wishlist = wishlist;
        this.rules = rules;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> target(String code) {
        return rules.findById(code).map(ProductTargetRuleEntity::getTarget);
    }

    @Transactional
    public void setTarget(String code, BigDecimal amount) {
        BigDecimal target = normalizePrice(amount);
        lockProduct(code);
        if (!wishlist.existsById(code)) {
            throw new IllegalArgumentException("Save this product to Wishlist before setting a price target.");
        }
        var existing = rules.findById(code);
        if (existing.isPresent()) existing.get().updateTarget(target);
        else rules.save(new ProductTargetRuleEntity(code, target));
    }

    @Transactional
    public void deleteTarget(String code) {
        lockProduct(code);
        rules.deleteById(code);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluateLive(ImportedCatalog catalog, Instant retrievedAt) {
        var enabled = rules.findWishlistedRulesForUpdate();
        if (enabled.isEmpty()) return;
        var imported = new HashMap<String, List<ImportedCatalog.ImportedListing>>();
        for (var product : catalog.products()) imported.put(product.code(), product.listings());

        Instant createdAt = clock.instant();
        for (var rule : enabled) {
            var known = imported.getOrDefault(rule.getProductCode(), List.of()).stream()
                    .filter(listing -> listing.price() != null).toList();
            if (known.isEmpty()) continue;
            var qualifying = known.stream().filter(listing -> listing.price().compareTo(rule.getTarget()) <= 0)
                    .sorted(Comparator.comparing(ImportedCatalog.ImportedListing::retailerCode))
                    .map(listing -> new ProductTargetRetailerPrice(listing.retailerCode(), listing.price()))
                    .toList();
            if (rule.evaluate(retrievedAt, !qualifying.isEmpty())) {
                events.save(new ProductTargetEventEntity(rule.getProductCode(), rule.getTarget(), retrievedAt,
                        createdAt, qualifying));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ProductTargetEventView> events(Instant now) {
        return events.findTop100ByOrderByCreatedAtDescIdDesc().stream()
                .map(event -> new ProductTargetEventView(event.getId(), event.getProductCode(), event.getTarget(),
                        event.getRetrievedAt(), event.getCreatedAt(), event.getRetailers(), event.getReadAt() != null,
                        !now.isBefore(event.getCreatedAt())
                                && (event.getSnoozedUntil() == null || !now.isBefore(event.getSnoozedUntil())),
                        event.getReadAt(), event.getSnoozedUntil()))
                .toList();
    }

    @Transactional
    public void markRead(long id) {
        requireEvent(id).markRead(clock.instant());
    }

    @Transactional
    public void snooze(long id) {
        requireEvent(id).snoozeUntil(clock.instant().plus(SNOOZE));
    }

    @Transactional
    public void reset() {
        events.deleteAll();
        rules.deleteAllInBatch();
    }

    private void lockProduct(String code) {
        products.findLockedByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
    }

    private ProductTargetEventEntity requireEvent(long id) {
        return events.findLocked(id).orElseThrow(() -> new IllegalArgumentException("Alert was not found."));
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException(INVALID_PRICE);
        }
        try {
            return price.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(INVALID_PRICE);
        }
    }
}
