package com.pricewatch.reminder;

import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.i18n.AppMessages;
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
import java.util.Comparator;
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
                .orElseThrow(() -> new IllegalArgumentException(AppMessages.text("error.reminder.product")));
        return ProductView.from(product);
    }

    @Transactional
    public void create(String productCode, int quantity, int intervalDays, String retailerCode) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException(AppMessages.text("error.quantity"));
        }
        if (intervalDays < 1) {
            throw new IllegalArgumentException(AppMessages.text("error.interval"));
        }
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException(AppMessages.text("error.reminder.product"));
        }

        ProductEntity product = products.findWithListingsByCode(productCode)
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException(AppMessages.text("error.reminder.product")));
        String selectedRetailer = retailerCode == null || retailerCode.isBlank() ? null : retailerCode.strip();
        if (selectedRetailer != null && product.getListings().stream()
                .map(RetailerListingEntity::getRetailerCode).noneMatch(selectedRetailer::equals)) {
            throw new IllegalArgumentException(AppMessages.text("error.reminder.retailer"));
        }

        Instant now = clock.instant();
        try {
            reminders.save(new ReminderEntity(product, quantity, intervalDays, selectedRetailer,
                    now.plus(intervalDays, ChronoUnit.DAYS)));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(AppMessages.text("error.intervalRange"), exception);
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
    public void changeRetailer(long id, String retailerCode) {
        ReminderEntity reminder = reminders.findWithProductById(id).orElseThrow(ReminderService::notFound);
        ProductEntity product = reminder.getProduct();
        if (!product.isActive()) {
            throw new IllegalArgumentException(AppMessages.text("error.product.inactive"));
        }

        String selectedRetailer = retailerCode == null || retailerCode.isBlank() ? null : retailerCode.strip();
        List<RetailerChoiceView> available = currentChoices(product);
        if (selectedRetailer == null) {
            if (available.isEmpty()) {
                throw new IllegalArgumentException(AppMessages.text("error.reminder.noPricedRetailer"));
            }
        } else if (available.stream().noneMatch(choice -> selectedRetailer.equals(choice.code()))) {
            throw new IllegalArgumentException(AppMessages.text("error.reminder.retailerPrice"));
        }
        reminder.setSelectedRetailerCode(selectedRetailer);
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
        List<RetailerChoiceView> available = currentChoices(product);
        boolean priceAvailable = available.stream()
                .anyMatch(choice -> retailerCode == null || retailerCode.equals(choice.code()));
        Instant effectiveDueAt = reminder.effectiveDueAt();
        String productName = name == null ? product.getCode()
                : LocalizedText.label(name.getEn(), name.getZhHant(), name.getZhHans());
        String brandName = brand == null ? "" : LocalizedText.label(brand.getEn(), brand.getZhHant(), brand.getZhHans());
        return new ReminderView(reminder.getId(), product.getCode(), productName, brandName,
                reminder.getQuantity(), reminder.getIntervalDays(),
                retailerCode, priceAvailable, product.isActive() && priceAvailable, available,
                product.isActive(), reminder.getNextDueAt(), effectiveDueAt,
                !quietHours && !now.isBefore(effectiveDueAt));
    }

    private static List<RetailerChoiceView> currentChoices(ProductEntity product) {
        return product.getListings().stream()
                .filter(listing -> listing.getPrice() != null)
                .sorted(Comparator.comparing(RetailerListingEntity::getRetailerCode))
                .map(listing -> new RetailerChoiceView(listing.getRetailerCode(), listing.getPrice()))
                .toList();
    }

    private static IllegalArgumentException notFound() {
        return new IllegalArgumentException(AppMessages.text("error.reminder.notFound"));
    }
}
