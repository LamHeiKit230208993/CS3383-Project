package com.pricewatch.tracking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * J05: explicit tracking choice for one exact product and retailer (D01).
 * A re-enable starts a new period (D02); only one row is enabled per pair at a
 * time. product/retailer are copied strings, not FKs, so catalogue replacement
 * cannot delete history through them.
 */
@Entity
@Table(name = "tracking_choices", uniqueConstraints = @UniqueConstraint(
        name = "uk_tracking_product_retailer", columnNames = {"product_code", "retailer_code"}))
public class TrackingChoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "retailer_code", length = 80, nullable = false)
    private String retailerCode;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "period_id", nullable = false)
    private long periodId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected TrackingChoiceEntity() {
    }

    public TrackingChoiceEntity(String productCode, String retailerCode, long periodId, Instant startedAt) {
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.enabled = true;
        this.periodId = periodId;
        this.startedAt = startedAt;
    }

    public void enable(long newPeriodId, Instant at) {
        this.enabled = true;
        this.periodId = newPeriodId;
        this.startedAt = at;
        this.endedAt = null;
    }

    public void disable(Instant at) {
        this.enabled = false;
        this.endedAt = at;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public boolean isEnabled() { return enabled; }
    public long getPeriodId() { return periodId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
}
