package com.pricewatch.reminder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "reminder_occurrences", uniqueConstraints =
        @UniqueConstraint(name = "uk_reminder_occurrence", columnNames = {"reminder_id", "due_at"}))
public class ReminderOccurrenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The history row can outlive its rule.
    @Column(name = "reminder_id", nullable = false)
    private long reminderId;
    @Column(name = "product_code", nullable = false, length = 80)
    private String productCode;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "retailer_code", length = 80)
    private String retailerCode;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    protected ReminderOccurrenceEntity() {
    }

    public ReminderOccurrenceEntity(ReminderEntity reminder, Instant now) {
        reminderId = reminder.getId();
        productCode = reminder.getProduct().getCode();
        quantity = reminder.getQuantity();
        retailerCode = reminder.getSelectedRetailerCode();
        dueAt = reminder.getNextDueAt();
        createdAt = now;
        snoozedUntil = reminder.getSnoozedUntil();
    }

    public void dismiss(Instant now) {
        if (readAt == null) readAt = now;
        snoozedUntil = null;
    }

    public void snoozeUntil(Instant until) {
        snoozedUntil = until;
    }

    public void revise(int quantity, String retailerCode) {
        this.quantity = quantity;
        this.retailerCode = retailerCode;
    }

    public Long getId() { return id; }
    public long getReminderId() { return reminderId; }
    public String getProductCode() { return productCode; }
    public int getQuantity() { return quantity; }
    public String getRetailerCode() { return retailerCode; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public Instant getSnoozedUntil() { return snoozedUntil; }
}
