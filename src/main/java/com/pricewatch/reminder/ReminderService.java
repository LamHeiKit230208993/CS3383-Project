package com.pricewatch.reminder;

import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.NotificationPolicy;
import com.pricewatch.reminder.persistence.ReminderEntity;
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

@Service
public class ReminderService {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 999;

    private final ReminderRepository reminders;
    private final ProductRepository products;
    private final ShoppingService shopping;
    private final Clock clock;
    private final NotificationPolicy notificationPolicy;

    public ReminderService(ReminderRepository reminders, ProductRepository products, ShoppingService shopping,
                           Clock clock, NotificationPolicy notificationPolicy) {
        this.reminders = reminders;
        this.products = products;
        this.shopping = shopping;
        this.clock = clock;
        this.notificationPolicy = notificationPolicy;
    }

    @Transactional(readOnly = true)
    public ReminderOverview overview() {
        Instant now = clock.instant();
        boolean quietHours = notificationPolicy.isQuiet(now);
        List<ReminderView> views = reminders.findAllByOrderByIdAsc().stream()
                .map(reminder -> toView(reminder, now, quietHours))
                .toList();
        return new ReminderOverview(views, quietHours, notificationPolicy.nextAllowed(now));
    }

    @Transactional(readOnly = true)
    public ProductView candidate(String code) {
        ProductEntity product = products.findWithListingsByCode(code)
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Choose a product from the current catalogue."));
        return ProductView.from(product);
    }

    @Transactional
    public void create(String productCode, int quantity, int intervalDays, String retailerCode) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 999.");
        }
        if (intervalDays < 1) {
            throw new IllegalArgumentException("Choose an interval of at least 1 day.");
        }
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("Choose a product from the current catalogue.");
        }

        ProductEntity product = products.findWithListingsByCode(productCode)
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Choose a product from the current catalogue."));
        String selectedRetailer = retailerCode == null || retailerCode.isBlank() ? null : retailerCode.strip();
        if (selectedRetailer != null && product.getListings().stream()
                .map(RetailerListingEntity::getRetailerCode).noneMatch(selectedRetailer::equals)) {
            throw new IllegalArgumentException("Choose a listed retailer for this product, or use Automatic lowest.");
        }

        Instant now = clock.instant();
        try {
            reminders.save(new ReminderEntity(product, quantity, intervalDays, selectedRetailer,
                    now.plus(intervalDays, ChronoUnit.DAYS)));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("That interval is outside the supported date range.", exception);
        }
    }

    @Transactional
    public void clear(long id) {
        reminders.findById(id).orElseThrow(ReminderService::notFound).clear(clock.instant());
    }

    @Transactional
    public void snooze(long id) {
        reminders.findById(id).orElseThrow(ReminderService::notFound).snooze(clock.instant());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void addToList(long id) {
        ReminderEntity reminder = reminders.findWithProductById(id).orElseThrow(ReminderService::notFound);
        shopping.add(reminder.getProduct().getCode(), reminder.getQuantity(), reminder.getSelectedRetailerCode());
    }

    @Transactional
    public void delete(long id) {
        ReminderEntity reminder = reminders.findById(id).orElseThrow(ReminderService::notFound);
        reminders.delete(reminder);
    }

    private ReminderView toView(ReminderEntity reminder, Instant now, boolean quietHours) {
        ProductEntity product = reminder.getProduct();
        var name = product.getName();
        var brand = product.getBrand();
        String retailerCode = reminder.getSelectedRetailerCode();
        boolean priceAvailable = product.getListings().stream().anyMatch(listing ->
                (retailerCode == null || retailerCode.equals(listing.getRetailerCode())) && listing.getPrice() != null);
        Instant effectiveDueAt = reminder.effectiveDueAt();
        String productName = name == null ? product.getCode()
                : label(name.getEn(), name.getZhHant(), name.getZhHans(), product.getCode());
        String brandName = brand == null ? "" : label(brand.getEn(), brand.getZhHant(), brand.getZhHans(), "");
        return new ReminderView(reminder.getId(), product.getCode(), productName, brandName,
                reminder.getQuantity(), reminder.getIntervalDays(),
                retailerCode, priceAvailable, product.isActive(), reminder.getNextDueAt(), effectiveDueAt,
                !quietHours && !now.isBefore(effectiveDueAt));
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
}
