package com.pricewatch.reminders.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reminders")
public class ReminderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "snooze_days", nullable = false)
    private int snoozeDays;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt;

    @Column(nullable = false)
    private boolean active;

    protected ReminderEntity() {
    }

    public ReminderEntity(String productCode, int quantity, int intervalDays, int snoozeDays,
                          Instant nextDueAt) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.intervalDays = intervalDays;
        this.snoozeDays = snoozeDays;
        this.nextDueAt = nextDueAt;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public int getQuantity() { return quantity; }
    public int getIntervalDays() { return intervalDays; }
    public int getSnoozeDays() { return snoozeDays; }
    public Instant getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Instant nextDueAt) { this.nextDueAt = nextDueAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
