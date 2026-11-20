package com.pricewatch.notifications.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_target_rules")
public class ProductTargetRuleEntity {
    @Id
    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal target;

    @Column(nullable = false)
    private boolean armed;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean paused;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    protected ProductTargetRuleEntity() {}

    public ProductTargetRuleEntity(String productCode, BigDecimal target) {
        this.productCode = productCode;
        this.target = target;
        this.armed = true;
    }

    public String getProductCode() { return productCode; }
    public BigDecimal getTarget() { return target; }
    public boolean isEnabled() { return !paused; }
    public boolean isArmed() { return armed; }
    public Instant getEvaluatedAt() { return evaluatedAt; }

    public void updateTarget(BigDecimal target) {
        if (this.target.compareTo(target) == 0) return;
        this.target = target;
        this.armed = true;
    }

    public void setEnabled(boolean enabled) {
        this.paused = !enabled;
    }

    public boolean evaluate(Instant retrievedAt, boolean hasQualifyingPrice) {
        if (evaluatedAt != null && retrievedAt.isBefore(evaluatedAt)) return false;
        evaluatedAt = retrievedAt;
        boolean shouldFire = armed && hasQualifyingPrice;
        armed = !hasQualifyingPrice;
        return shouldFire;
    }
}
