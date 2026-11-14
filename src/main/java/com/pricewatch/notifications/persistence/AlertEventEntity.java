package com.pricewatch.notifications.persistence;

import com.pricewatch.tracking.persistence.TrackingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "alert_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_alert_observation_type", columnNames = {
                "tracking_period_id", "observation_id", "event_type"}),
        @UniqueConstraint(name = "uk_alert_summary_date", columnNames = "summary_date")
})
public class AlertEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_period_id")
    private TrackingEntity tracking;

    @Column(name = "observation_id")
    private Long observationId;

    @Column(name = "event_type", nullable = false, length = 16)
    private String type;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "observed_at")
    private Instant observedAt;

    @Column(name = "previous_price", precision = 19, scale = 4)
    private BigDecimal previousPrice;

    @Column(name = "previous_observed_at")
    private Instant previousObservedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @Column(name = "summary_date")
    private LocalDate summaryDate;

    @Column(name = "summary_count")
    private Integer summaryCount;

    @Column(name = "summarized_at")
    private Instant summarizedAt;

    protected AlertEventEntity() {
    }

    public AlertEventEntity(TrackingEntity tracking, Long observationId, String type, BigDecimal price,
                            Instant observedAt, BigDecimal previousPrice, Instant previousObservedAt,
                            Instant createdAt, Instant availableAt) {
        this.tracking = tracking;
        this.observationId = observationId;
        this.type = type;
        this.price = price;
        this.observedAt = observedAt;
        this.previousPrice = previousPrice;
        this.previousObservedAt = previousObservedAt;
        this.createdAt = createdAt;
        this.availableAt = availableAt;
    }

    public AlertEventEntity(LocalDate summaryDate, int summaryCount, Instant generatedAt) {
        this.type = "SUMMARY";
        this.summaryDate = summaryDate;
        this.summaryCount = summaryCount;
        this.createdAt = generatedAt;
        this.availableAt = generatedAt;
    }

    public Long getId() { return id; }
    public TrackingEntity getTracking() { return tracking; }
    public Long getObservationId() { return observationId; }
    public String getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public Instant getObservedAt() { return observedAt; }
    public BigDecimal getPreviousPrice() { return previousPrice; }
    public Instant getPreviousObservedAt() { return previousObservedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAvailableAt() { return availableAt; }
    public Instant getReadAt() { return readAt; }
    public Instant getSnoozedUntil() { return snoozedUntil; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public Integer getSummaryCount() { return summaryCount; }
    public Instant getSummarizedAt() { return summarizedAt; }

    public void markRead(Instant at) { this.readAt = at; }
    public void snoozeUntil(Instant at) { this.snoozedUntil = at; }
    public void markSummarized(Instant at) { this.summarizedAt = at; }
}
