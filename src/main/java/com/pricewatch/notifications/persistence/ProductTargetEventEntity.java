package com.pricewatch.notifications.persistence;

import com.pricewatch.notifications.WatchPriceKind;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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

    @Column(precision = 19, scale = 4)
    private BigDecimal target;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_kind", length = 16)
    private WatchPriceKind priceKind;

    @Column(name = "retrieved_at")
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

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "scheduled_due_at")
    private Instant scheduledDueAt;

    @Column(name = "legacy_occurrence_id", unique = true)
    private Long legacyOccurrenceId;

    @Column(name = "cart_quantity")
    private Integer cartQuantity;

    @Column(name = "cart_retailer", length = 80)
    private String cartRetailer;

    protected ProductTargetEventEntity() {}

    public ProductTargetEventEntity(String productCode, BigDecimal target, Instant retrievedAt,
                                    Instant createdAt, List<ProductTargetRetailerPrice> retailers) {
        this(productCode, target, retrievedAt, createdAt, retailers, null, null);
    }

    public ProductTargetEventEntity(String productCode, BigDecimal target, Instant retrievedAt,
                                    Instant createdAt, List<ProductTargetRetailerPrice> retailers,
                                    Long ruleId, Instant scheduledDueAt) {
        this.productCode = productCode;
        this.target = target;
        this.retrievedAt = retrievedAt;
        this.createdAt = createdAt;
        this.retailers.addAll(retailers);
        this.ruleId = ruleId;
        this.scheduledDueAt = scheduledDueAt;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public BigDecimal getTarget() { return target; }
    public WatchPriceKind getPriceKind() { return priceKind == null ? WatchPriceKind.TARGET : priceKind; }
    public void setPriceKind(WatchPriceKind priceKind) { this.priceKind = priceKind; }
    public Instant getRetrievedAt() { return retrievedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ProductTargetRetailerPrice> getRetailers() { return List.copyOf(retailers); }
    public Instant getReadAt() { return readAt; }
    public Instant getSnoozedUntil() { return snoozedUntil; }
    public Long getRuleId() { return ruleId; }
    public Instant getScheduledDueAt() { return scheduledDueAt; }
    public Integer getCartQuantity() { return cartQuantity; }
    public String getCartRetailer() { return cartRetailer; }

    public void reviseCartDefaults(int quantity, String retailer) {
        cartQuantity = quantity;
        cartRetailer = retailer;
    }

    public void markRead(Instant now) {
        if (readAt == null) readAt = now;
    }

    public void snoozeUntil(Instant until) {
        snoozedUntil = until;
    }
}
