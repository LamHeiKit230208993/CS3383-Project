package com.pricewatch.alerts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * J07: singleton planned-budget settings (ID 1). A weekly/monthly label plus a
 * planned limit for the whole My list estimate (D06). This is planned cost, not
 * actual spending.
 */
@Entity
@Table(name = "budget_settings")
public class BudgetSettingsEntity {
    public enum Period { WEEKLY, MONTHLY }

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Period period;

    @Column(name = "planned_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedLimit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetSettingsEntity() {
    }

    public BudgetSettingsEntity(Period period, BigDecimal plannedLimit, Instant updatedAt) {
        this.id = SINGLETON_ID;
        this.period = period;
        this.plannedLimit = plannedLimit;
        this.updatedAt = updatedAt;
    }

    public void update(Period period, BigDecimal plannedLimit, Instant at) {
        this.period = period;
        this.plannedLimit = plannedLimit;
        this.updatedAt = at;
    }

    public Long getId() { return id; }
    public Period getPeriod() { return period; }
    public BigDecimal getPlannedLimit() { return plannedLimit; }
    public Instant getUpdatedAt() { return updatedAt; }
}
