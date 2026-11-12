package com.pricewatch.alerts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alert_events")
public class AlertEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "retailer_code", length = 80, nullable = false)
    private String retailerCode;

    @Column(name = "rule_type", length = 16, nullable = false)
    private String ruleType;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    protected AlertEventEntity() {
    }

    public AlertEventEntity(Long ruleId, Long observationId, String productCode, String retailerCode,
                            String ruleType, BigDecimal price, Instant firedAt) {
        this.ruleId = ruleId;
        this.observationId = observationId;
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.ruleType = ruleType;
        this.price = price;
        this.firedAt = firedAt;
    }

    public Long getId() { return id; }
    public Long getRuleId() { return ruleId; }
    public Long getObservationId() { return observationId; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public String getRuleType() { return ruleType; }
    public BigDecimal getPrice() { return price; }
    public Instant getFiredAt() { return firedAt; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public Instant getSnoozedUntil() { return snoozedUntil; }
    public void setSnoozedUntil(Instant snoozedUntil) { this.snoozedUntil = snoozedUntil; }
}
