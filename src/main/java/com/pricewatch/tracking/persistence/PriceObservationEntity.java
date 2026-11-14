package com.pricewatch.tracking.persistence;

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
@Table(name = "price_observations")
public class PriceObservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracking_period_id", nullable = false)
    private TrackingEntity tracking;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "source_checksum", nullable = false, length = 64)
    private String sourceChecksum;

    protected PriceObservationEntity() {
    }

    public PriceObservationEntity(TrackingEntity tracking, BigDecimal price, Instant observedAt,
                                  String sourceUrl, String sourceChecksum) {
        this.tracking = tracking;
        this.price = price;
        this.observedAt = observedAt;
        this.sourceUrl = sourceUrl;
        this.sourceChecksum = sourceChecksum;
    }

    public Long getId() { return id; }
    public TrackingEntity getTracking() { return tracking; }
    public BigDecimal getPrice() { return price; }
    public Instant getObservedAt() { return observedAt; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSourceChecksum() { return sourceChecksum; }
}
