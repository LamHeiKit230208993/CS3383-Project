package com.pricewatch.reminder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * J08: a replenishment reminder for one exact product (D07). The user sets an
 * interval in days; due = created + interval. Snooze moves due to now + 24h;
 * adding to the list clears the reminder and sets the next due = now + interval.
 * One reminder per product is kept simple for this batch.
 */
@Entity
@Table(name = "reminders", uniqueConstraints = @UniqueConstraint(
        name = "uk_reminder_product", columnNames = {"product_code"}))
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

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReminderEntity() {
    }

    public ReminderEntity(String productCode, int quantity, int intervalDays, Instant dueAt, Instant createdAt) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.intervalDays = intervalDays;
        this.dueAt = dueAt;
        this.createdAt = createdAt;
    }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public int getQuantity() { return quantity; }
    public int getIntervalDays() { return intervalDays; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCreatedAt() { return createdAt; }
}
