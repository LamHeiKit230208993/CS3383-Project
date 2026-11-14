package com.pricewatch.tracking.persistence;

import com.pricewatch.catalog.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_tracking_periods")
public class TrackingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_code", nullable = false)
    private ProductEntity product;

    @Column(name = "retailer_code", nullable = false, length = 80)
    private String retailerCode;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "target_price", precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "target_armed", nullable = false)
    private boolean targetArmed = true;

    @Column(name = "drop_enabled", nullable = false)
    private boolean dropEnabled;

    @Column(name = "has_observed_price_state", nullable = false)
    private boolean hasObservedPriceState;

    @Column(name = "last_observed_price", precision = 19, scale = 4)
    private BigDecimal lastObservedPrice;

    @Column(name = "last_observed_at")
    private Instant lastObservedAt;

    protected TrackingEntity() {
    }

    public TrackingEntity(ProductEntity product, String retailerCode, Instant startedAt) {
        this.product = product;
        this.retailerCode = retailerCode;
        this.startedAt = startedAt;
        this.enabled = true;
    }

    public Long getId() { return id; }
    public ProductEntity getProduct() { return product; }
    public String getProductCode() { return product.getCode(); }
    public String getRetailerCode() { return retailerCode; }
    public boolean isEnabled() { return enabled; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getStoppedAt() { return stoppedAt; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public boolean isTargetArmed() { return targetArmed; }
    public boolean isDropEnabled() { return dropEnabled; }
    public boolean hasObservedPriceState() { return hasObservedPriceState; }
    public BigDecimal getLastObservedPrice() { return lastObservedPrice; }
    public Instant getLastObservedAt() { return lastObservedAt; }

    public void stop(Instant stoppedAt) {
        this.enabled = false;
        this.stoppedAt = stoppedAt;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
        this.targetArmed = true;
    }

    public void setTargetArmed(boolean targetArmed) {
        this.targetArmed = targetArmed;
    }

    public void setDropEnabled(boolean dropEnabled) {
        this.dropEnabled = dropEnabled;
    }

    public void recordPriceState(BigDecimal price, Instant observedAt) {
        this.hasObservedPriceState = true;
        this.lastObservedPrice = price;
        this.lastObservedAt = observedAt;
    }

    public void clearPriceState() {
        this.hasObservedPriceState = false;
        this.lastObservedPrice = null;
        this.lastObservedAt = null;
    }
}
