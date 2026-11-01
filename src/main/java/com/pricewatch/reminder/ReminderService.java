package com.pricewatch.reminder;

import com.pricewatch.reminder.persistence.ReminderEntity;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * J08: replenishment reminders (D07). A reminder stores a product, quantity, and
 * a user-set interval in days. Add-to-list reuses {@link ShoppingService#add} so
 * the 1–999 quantity limit and the existing-item/retailer conflict rules are not
 * re-implemented. Clear deletes; snooze moves due to now + 24h; add-to-list sets
 * the next due to now + interval. Evaluation happens on page load via {@link #dueReminders()}.
 */
@Service
public class ReminderService {
    private static final Duration SNOOZE = Duration.ofHours(24);

    private final ReminderRepository reminders;
    private final ShoppingService shopping;
    private final Clock clock;

    public ReminderService(ReminderRepository reminders, ShoppingService shopping, Clock clock) {
        this.reminders = reminders;
        this.shopping = shopping;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReminderEntity> all() {
        return reminders.findAllByOrderByDueAtAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<ReminderEntity> dueReminders() {
        return reminders.findAllByDueAtLessThanEqualOrderByDueAtAscIdAsc(clock.instant());
    }

    @Transactional
    public void create(String productCode, int quantity, int intervalDays) {
        if (intervalDays < 1) {
            throw new IllegalArgumentException("Set the reminder interval to at least 1 day.");
        }
        if (quantity < 1 || quantity > 999) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 999.");
        }
        Instant now = clock.instant();
        ReminderEntity existing = reminders.findByProductCode(productCode).orElse(null);
        if (existing == null) {
            reminders.save(new ReminderEntity(productCode, quantity, intervalDays, now.plus(Duration.ofDays(intervalDays)), now));
        } else {
            existing.setQuantity(quantity);
            existing.setIntervalDays(intervalDays);
            existing.setDueAt(now.plus(Duration.ofDays(intervalDays)));
        }
    }

    /** Add the reminder's quantity to My list, then reschedule it. */
    @Transactional
    public void addToList(String productCode) {
        ReminderEntity reminder = reminders.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("That reminder does not exist."));
        shopping.add(productCode, reminder.getQuantity(), null);
        Instant now = clock.instant();
        reminder.setDueAt(now.plus(Duration.ofDays(reminder.getIntervalDays())));
    }

    @Transactional
    public void clear(String productCode) {
        reminders.findByProductCode(productCode).ifPresent(reminders::delete);
    }

    @Transactional
    public void snooze(String productCode) {
        reminders.findByProductCode(productCode)
                .ifPresent(reminder -> reminder.setDueAt(clock.instant().plus(SNOOZE)));
    }
}
