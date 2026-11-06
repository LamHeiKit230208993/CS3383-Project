package com.pricewatch.budget.persistence;

import com.pricewatch.budget.BudgetPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_settings")
public class BudgetSettingsEntity {
    private static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_period", nullable = false, length = 16)
    private BudgetPeriod period;

    @Column(name = "planned_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal limitAmount;

    protected BudgetSettingsEntity() {
    }

    public BudgetSettingsEntity(BudgetPeriod period, BigDecimal limitAmount) {
        this.id = SINGLETON_ID;
        this.period = period;
        this.limitAmount = limitAmount;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }
}
