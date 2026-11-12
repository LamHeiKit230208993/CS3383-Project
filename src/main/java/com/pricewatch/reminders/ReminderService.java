package com.pricewatch.reminders;

import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.reminders.persistence.ReminderEntity;
import com.pricewatch.reminders.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final ProductRepository productRepository;
    private final ShoppingService shopping;
    private final TransactionTemplate transactionTemplate;
    private final Object mutationLock = new Object();
    private final Clock clock;

    public ReminderService(ReminderRepository reminderRepository,
                           ProductRepository productRepository,
                           ShoppingService shopping,
                           PlatformTransactionManager transactionManager,
                           Clock clock) {
        this.reminderRepository = reminderRepository;
        this.productRepository = productRepository;
        this.shopping = shopping;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public void create(String productCode, int quantity, int intervalDays, int snoozeDays) {
        if (quantity < 1 || quantity > 999) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 999.");
        }
        if (intervalDays < 1) {
            throw new IllegalArgumentException("Interval must be at least one day.");
        }
        if (snoozeDays < 1) {
            throw new IllegalArgumentException("Snooze must be at least one day.");
        }
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> {
                productRepository.findById(productCode)
                        .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
                ReminderEntity reminder = new ReminderEntity(productCode, quantity, intervalDays, snoozeDays,
                        clock.instant().plus(intervalDays, ChronoUnit.DAYS));
                reminderRepository.save(reminder);
            });
        }
    }

    /**
     * Add the reminder's product and quantity to My list. Reuses J04 add rules
     * (quantity 1–999 and the existing-item/other-retailer conflict). Does not
     * change reminder state (D07).
     */
    public void addToList(Long reminderId) {
        ReminderEntity reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("That reminder was not found."));
        shopping.add(reminder.getProductCode(), reminder.getQuantity());
    }

    /** Clear deactivates the reminder (D07). */
    public void clear(Long reminderId) {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status ->
                    reminderRepository.findById(reminderId).ifPresent(reminder -> {
                        reminder.setActive(false);
                        reminderRepository.save(reminder);
                    }));
        }
    }

    /** Snooze pushes the next due date by the reminder's snooze period. */
    public void snooze(Long reminderId) {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status ->
                    reminderRepository.findById(reminderId).ifPresent(reminder -> {
                        reminder.setNextDueAt(reminder.getNextDueAt().plus(reminder.getSnoozeDays(), ChronoUnit.DAYS));
                        reminderRepository.save(reminder);
                    }));
        }
    }

    @Transactional(readOnly = true)
    public List<ReminderView> reminders() {
        var now = clock.instant();
        return reminderRepository.findAllByOrderByIdAsc().stream()
                .map(reminder -> new ReminderView(
                        reminder.getId(),
                        reminder.getProductCode(),
                        productName(reminder.getProductCode()),
                        reminder.getQuantity(),
                        reminder.getIntervalDays(),
                        reminder.getSnoozeDays(),
                        reminder.getNextDueAt(),
                        reminder.isActive(),
                        reminder.isActive() && !reminder.getNextDueAt().isAfter(now)))
                .toList();
    }

    private String productName(String code) {
        return productRepository.findById(code)
                .map(ProductEntity::getName)
                .map(name -> name == null ? code : name.getEn())
                .orElse(code);
    }
}
