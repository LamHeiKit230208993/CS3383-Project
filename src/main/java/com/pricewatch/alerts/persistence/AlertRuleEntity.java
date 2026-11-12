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

@Entity
@Table(name = "alert_rules")
public class AlertRuleEntity {
    public enum Type { TARGET, DROP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "retailer_code", length = 80, nullable = false)
    private String retailerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 16)
    private Type type;

    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_fired_observation_id")
    private Long lastFiredObservationId;

    protected AlertRuleEntity() {
    }

    public AlertRuleEntity(String productCode, String retailerCode, Type type, BigDecimal targetAmount) {
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.type = type;
        this.targetAmount = targetAmount;
        this.enabled = true;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public Type getType() { return type; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Long getLastFiredObservationId() { return lastFiredObservationId; }
    public void setLastFiredObservationId(Long id) { this.lastFiredObservationId = id; }
}
