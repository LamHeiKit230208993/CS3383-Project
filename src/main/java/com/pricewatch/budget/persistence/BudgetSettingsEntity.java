package com.pricewatch.budget.persistence;

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
    public enum Period { WEEKLY, MONTHLY }

    @Id
    private Long id = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_period", nullable = false, length = 16)
    private Period period;

    @Column(name = "limit_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal limitAmount;

    protected BudgetSettingsEntity() {
    }

    public BudgetSettingsEntity(Period period, BigDecimal limitAmount) {
        this.period = period;
        this.limitAmount = limitAmount;
    }

    public Long getId() { return id; }
    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
}
