package com.pricewatch.notifications.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_target_events")
public class ProductTargetEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal target;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "product_target_event_prices", joinColumns = @JoinColumn(name = "event_id"))
    @OrderBy("retailerCode")
    private List<ProductTargetRetailerPrice> retailers = new ArrayList<>();

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    protected ProductTargetEventEntity() {}

    public ProductTargetEventEntity(String productCode, BigDecimal target, Instant retrievedAt,
                                    Instant createdAt, List<ProductTargetRetailerPrice> retailers) {
        this.productCode = productCode;
        this.target = target;
        this.retrievedAt = retrievedAt;
        this.createdAt = createdAt;
        this.retailers.addAll(retailers);
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public BigDecimal getTarget() { return target; }
    public Instant getRetrievedAt() { return retrievedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ProductTargetRetailerPrice> getRetailers() { return List.copyOf(retailers); }
    public Instant getReadAt() { return readAt; }
    public Instant getSnoozedUntil() { return snoozedUntil; }

    public void markRead(Instant now) {
        if (readAt == null) readAt = now;
    }

    public void snoozeUntil(Instant until) {
        snoozedUntil = until;
    }
}
