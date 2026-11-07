package com.pricewatch.alerts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * J06: an alert rule for one tracked product, independent of retailer. TARGET
 * fires when the lowest current known price across the product's tracked
 * retailers is at or below {@code targetAmount} (D04). DROP fires when any
 * retailer's new known observation is lower than its latest prior known
 * observation.
 */
@Entity
@Table(name = "alert_rules")
public class AlertRuleEntity {
    public enum Type { TARGET, DROP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private boolean enabled;

    protected AlertRuleEntity() {
    }

    public AlertRuleEntity(String productCode, Type type, BigDecimal targetAmount) {
        this.productCode = productCode;
        this.type = type;
        this.targetAmount = targetAmount;
        this.enabled = true;
    }

    public void disable() { this.enabled = false; }
    public void enable() { this.enabled = true; }

    public void update(Type type, BigDecimal targetAmount) {
        this.type = type;
        this.targetAmount = type == Type.TARGET ? targetAmount : null;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public Type getType() { return type; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public boolean isEnabled() { return enabled; }
}
