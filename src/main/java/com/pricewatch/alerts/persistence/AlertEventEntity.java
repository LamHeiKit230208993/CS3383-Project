package com.pricewatch.alerts.persistence;

import com.pricewatch.alerts.persistence.AlertRuleEntity.Type;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * J06: a persisted alert event. {@code visible} marks that the shopper actually
 * saw it inside the running app (D05). Quiet-hours events are created but held
 * not-visible until shown. {@code snoozeUntil} hides an event until that time.
 * {@code deliveredWhileClosed} is always false: a closed app never delivers.
 */
@Entity
@Table(name = "alert_events")
public class AlertEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "retailer_code", length = 80, nullable = false)
    private String retailerCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(precision = 19, scale = 4)
    private BigDecimal comparedPrice;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "delivered_while_closed", nullable = false)
    private boolean deliveredWhileClosed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "snooze_until")
    private Instant snoozeUntil;

    protected AlertEventEntity() {
    }

    public AlertEventEntity(String productCode, String retailerCode, Type type,
                            BigDecimal price, BigDecimal comparedPrice, Instant createdAt) {
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.type = type;
        this.price = price;
        this.comparedPrice = comparedPrice;
        this.createdAt = createdAt;
        this.visible = false;
        this.deliveredWhileClosed = false;
    }

    public void markVisible() { this.visible = true; }
    public void snooze(Instant until) { this.snoozeUntil = until; }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public Type getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getComparedPrice() { return comparedPrice; }
    public boolean isVisible() { return visible; }
    public boolean isDeliveredWhileClosed() { return deliveredWhileClosed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSnoozeUntil() { return snoozeUntil; }
}
