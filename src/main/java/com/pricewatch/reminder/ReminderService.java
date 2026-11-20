package com.pricewatch.reminder;

import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.reminder.persistence.ReminderEntity;
import com.pricewatch.reminder.persistence.ReminderOccurrenceEntity;
import com.pricewatch.reminder.repository.ReminderOccurrenceRepository;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReminderService {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 999;

    private final ReminderRepository reminders;
    private final ProductRepository products;
    private final ShoppingService shopping;
    private final Clock clock;
    private final ReminderOccurrenceRepository history;

    public ReminderService(ReminderRepository reminders, ProductRepository products, ShoppingService shopping,
                           Clock clock, ReminderOccurrenceRepository history) {
        this.reminders = reminders;
        this.products = products;
        this.shopping = shopping;
        this.clock = clock;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public ReminderOverview overview() {
        Instant now = clock.instant();
        List<ReminderView> views = reminders.findAllByOrderByIdAsc().stream()
                .map(reminder -> toView(reminder, now))
                .toList();
        return new ReminderOverview(views);
    }

    @Transactional
    public void recordDue() {
        Instant now = clock.instant();
        for (long id : reminders.dueRuleIds(now)) {
            reminders.findLockedById(id).ifPresent(reminder -> currentOccurrence(reminder, now));
        }
    }

    @Transactional(readOnly = true)
    public List<ReminderOccurrenceView> occurrences(Instant now) {
        Map<Long, ReminderEntity> currentRules = reminders.findAll().stream()
                .collect(Collectors.toMap(ReminderEntity::getId, Function.identity()));
        return history.findAllByOrderByCreatedAtDescIdDesc().stream().map(event -> {
            ReminderEntity rule = currentRules.get(event.getReminderId());
            boolean active = isCurrent(event, rule);
            boolean available = event.getSnoozedUntil() == null || !now.isBefore(event.getSnoozedUntil());
            boolean priceAvailable = active && products.findWithListingsByCode(event.getProductCode())
                    .filter(ProductEntity::isActive)
                    .filter(product -> hasKnownPrice(product, event.getRetailerCode())).isPresent();
            return new ReminderOccurrenceView(event.getId(), event.getReminderId(), event.getProductCode(),
                    event.getQuantity(), event.getRetailerCode(), event.getDueAt(), event.getCreatedAt(),
                    event.getReadAt() != null, available, active, priceAvailable);
        }).toList();
    }

    @Transactional
    public void create(String productCode, int quantity, int intervalDays, String retailerCode) {
        validateRule(quantity, intervalDays);
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("Choose a product from the current catalogue.");
        }
        ProductEntity product = products.findWithListingsByCode(productCode)
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Choose a product from the current catalogue."));
        String selectedRetailer = retailer(retailerCode);
        validateRetailer(product, selectedRetailer);
        try {
            reminders.save(new ReminderEntity(product, quantity, intervalDays, selectedRetailer,
                    clock.instant().plus(intervalDays, ChronoUnit.DAYS)));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("That interval is outside the supported date range.", exception);
        }
    }

    @Transactional
    public void update(long id, int quantity, int intervalDays, String retailerCode) {
        validateRule(quantity, intervalDays);
        ReminderEntity reminder = lockedRule(id);
        String selectedRetailer = retailer(retailerCode);
        if (!Objects.equals(selectedRetailer, reminder.getSelectedRetailerCode())) {
            validateRetailer(reminder.getProduct(), selectedRetailer);
        }
        Instant now = clock.instant();
        if (intervalDays != reminder.getIntervalDays()) {
            dismissCurrent(reminder, now);
        } else {
            history.findByReminderIdAndDueAt(id, reminder.getNextDueAt())
                    .filter(event -> event.getReadAt() == null)
                    .ifPresent(event -> event.revise(quantity, selectedRetailer));
        }
        try {
            reminder.updateRule(quantity, intervalDays, selectedRetailer, now);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("That interval is outside the supported date range.", exception);
        }
    }

    @Transactional
    public void setEnabled(long id, boolean enabled) {
        lockedRule(id).setEnabled(enabled);
    }

    private void validateRule(int quantity, int intervalDays) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 999.");
        }
        if (intervalDays < 1) {
            throw new IllegalArgumentException("Choose an interval of at least 1 day.");
        }
    }

    private void validateRetailer(ProductEntity product, String selectedRetailer) {
        if (selectedRetailer != null && product.getListings().stream()
                .map(RetailerListingEntity::getRetailerCode).noneMatch(selectedRetailer::equals)) {
            throw new IllegalArgumentException("Choose a listed retailer for this product, or use Automatic lowest.");
        }

    }

    private String retailer(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Transactional
    public void clear(long id) {
        ReminderEntity reminder = requireEnabled(lockedRule(id));
        Instant now = clock.instant();
        dismissCurrent(reminder, now);
        reminder.clear(now);
    }

    @Transactional
    public void snooze(long id) {
        ReminderEntity reminder = requireEnabled(lockedRule(id));
        Instant now = clock.instant();
        var occurrence = currentOccurrence(reminder, now);
        reminder.snooze(now);
        occurrence.ifPresent(event -> event.snoozeUntil(reminder.getSnoozedUntil()));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void addToList(long id) {
        ReminderEntity reminder = requireEnabled(reminders.findWithProductById(id).orElseThrow(ReminderService::notFound));
        shopping.add(reminder.getProduct().getCode(), reminder.getQuantity(), reminder.getSelectedRetailerCode());
    }

    @Transactional
    public void delete(long id) {
        ReminderEntity reminder = lockedRule(id);
        dismissCurrent(reminder, clock.instant());
        reminders.delete(reminder);
    }

    @Transactional
    public void clearOccurrence(long id) {
        var current = currentAlert(id);
        current.event().dismiss(current.now());
        current.rule().clear(current.now());
    }

    @Transactional
    public void snoozeOccurrence(long id) {
        var current = availableAlert(id);
        current.rule().snooze(current.now());
        current.event().snoozeUntil(current.rule().getSnoozedUntil());
    }

    @Transactional
    public void addOccurrence(long id) {
        var current = availableAlert(id);
        var rule = current.rule();
        shopping.addFromReminder(rule.getProduct().getCode(), rule.getQuantity(), rule.getSelectedRetailerCode());
    }

    private CurrentAlert availableAlert(long id) {
        var current = currentAlert(id);
        if (current.now().isBefore(current.rule().effectiveDueAt())) throw alertNotFound();
        return current;
    }

    private CurrentAlert currentAlert(long id) {
        var event = history.findById(id).orElseThrow(ReminderService::alertNotFound);
        var rule = reminders.findLockedById(event.getReminderId()).orElseThrow(ReminderService::alertNotFound);
        Instant now = clock.instant();
        if (!isCurrent(event, rule)) throw alertNotFound();
        return new CurrentAlert(event, rule, now);
    }

    private boolean isCurrent(ReminderOccurrenceEntity event, ReminderEntity rule) {
        return rule != null && rule.isEnabled() && event.getReadAt() == null && event.getDueAt().equals(rule.getNextDueAt());
    }

    private record CurrentAlert(ReminderOccurrenceEntity event, ReminderEntity rule, Instant now) {}

    private ReminderEntity lockedRule(long id) {
        return reminders.findLockedById(id).orElseThrow(ReminderService::notFound);
    }

    private static ReminderEntity requireEnabled(ReminderEntity reminder) {
        if (!reminder.isEnabled()) throw alertNotFound();
        return reminder;
    }

    private Optional<ReminderOccurrenceEntity> currentOccurrence(ReminderEntity reminder, Instant now) {
        var current = history.findByReminderIdAndDueAt(reminder.getId(), reminder.getNextDueAt());
        if (current.isPresent() || !reminder.isEnabled() || now.isBefore(reminder.effectiveDueAt())) return current;
        return Optional.of(history.save(new ReminderOccurrenceEntity(reminder, now)));
    }

    private void dismissCurrent(ReminderEntity reminder, Instant now) {
        currentOccurrence(reminder, now).ifPresent(event -> event.dismiss(now));
    }

    private ReminderView toView(ReminderEntity reminder, Instant now) {
        ProductEntity product = reminder.getProduct();
        var name = product.getName();
        var brand = product.getBrand();
        String retailerCode = reminder.getSelectedRetailerCode();
        boolean priceAvailable = hasKnownPrice(product, retailerCode);
        Instant effectiveDueAt = reminder.effectiveDueAt();
        String productName = name == null ? product.getCode()
                : label(name.getEn(), name.getZhHant(), name.getZhHans(), product.getCode());
        String brandName = brand == null ? "" : label(brand.getEn(), brand.getZhHant(), brand.getZhHans(), "");
        return new ReminderView(reminder.getId(), product.getCode(), productName, brandName,
                reminder.getQuantity(), reminder.getIntervalDays(),
                retailerCode, priceAvailable, product.isActive(), reminder.getNextDueAt(), effectiveDueAt,
                reminder.isEnabled() && !now.isBefore(effectiveDueAt), reminder.isEnabled());
    }

    private boolean hasKnownPrice(ProductEntity product, String retailerCode) {
        return product.getListings().stream().anyMatch(listing ->
                (retailerCode == null || retailerCode.equals(listing.getRetailerCode())) && listing.getPrice() != null);
    }

    private static String label(String english, String traditional, String simplified, String fallback) {
        if (english != null && !english.isBlank()) return english;
        if (traditional != null && !traditional.isBlank()) return traditional;
        if (simplified != null && !simplified.isBlank()) return simplified;
        return fallback;
    }

    private static IllegalArgumentException notFound() {
        return new IllegalArgumentException("That reminder no longer exists.");
    }

    private static IllegalArgumentException alertNotFound() {
        return new IllegalArgumentException("Alert was not found.");
    }
}
