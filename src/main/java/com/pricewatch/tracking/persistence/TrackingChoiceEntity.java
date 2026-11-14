package com.pricewatch.tracking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

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

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    protected TrackingChoiceEntity() {
    }

    public TrackingChoiceEntity(String productCode, String retailerCode, Instant startedAt) {
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.enabled = true;
        this.startedAt = startedAt;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getStartedAt() { return startedAt; }
}
