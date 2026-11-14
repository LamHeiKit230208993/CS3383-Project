package com.pricewatch.reminder.persistence;

import com.pricewatch.catalog.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "replenishment_reminders")
public class ReminderEntity {
    private static final long SNOOZE_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_code", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "selected_retailer_code", length = 80)
    private String selectedRetailerCode;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    protected ReminderEntity() {
    }

    public ReminderEntity(ProductEntity product, int quantity, int intervalDays,
                          String selectedRetailerCode, Instant nextDueAt) {
        this.product = product;
        this.quantity = quantity;
        this.intervalDays = intervalDays;
        this.selectedRetailerCode = selectedRetailerCode;
        this.nextDueAt = nextDueAt;
    }

    public void clear(Instant now) {
        nextDueAt = now.plus(intervalDays, ChronoUnit.DAYS);
        snoozedUntil = null;
    }

    public void snooze(Instant now) {
        Instant currentDue = snoozedUntil != null && snoozedUntil.isAfter(nextDueAt)
                ? snoozedUntil : nextDueAt;
        snoozedUntil = (currentDue.isAfter(now) ? currentDue : now).plus(SNOOZE_HOURS, ChronoUnit.HOURS);
    }

    public Long getId() { return id; }
    public ProductEntity getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public int getIntervalDays() { return intervalDays; }
    public String getSelectedRetailerCode() { return selectedRetailerCode; }
    public Instant getNextDueAt() { return nextDueAt; }
    public Instant getSnoozedUntil() { return snoozedUntil; }

    public Instant effectiveDueAt() {
        return snoozedUntil != null && snoozedUntil.isAfter(nextDueAt) ? snoozedUntil : nextDueAt;
    }
}
