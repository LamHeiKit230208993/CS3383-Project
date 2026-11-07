package com.pricewatch.tracking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * J05/J06: one retained price observation for a tracked (product, retailer)
 * pair. Deliberately has no FK to retailer listings: catalogue replacement
 * deletes those rows, but history must survive (D02). Identity is
 * (product, retailer, checksum, period) per D03, so a repeated identical import
 * adds nothing. An unknown price is stored with known=false and price=null and
 * is used for provenance only (D03): it never feeds target/drop comparisons,
 * repeat-reset, or budget arithmetic.
 */
@Entity
@Table(name = "price_observations", uniqueConstraints = @UniqueConstraint(
        name = "uk_observation_identity",
        columnNames = {"product_code", "retailer_code", "checksum", "period_id"}))
public class PriceObservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "retailer_code", length = 80, nullable = false)
    private String retailerCode;

    @Column(name = "period_id", nullable = false)
    private long periodId;

    @Column(nullable = false)
    private boolean known;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(name = "source_date")
    private Instant sourceDate;

    protected PriceObservationEntity() {
    }

    public PriceObservationEntity(String productCode, String retailerCode, long periodId,
                                  BigDecimal price, String checksum, Instant retrievedAt, Instant sourceDate) {
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.periodId = periodId;
        this.known = price != null;
        this.price = price;
        this.checksum = checksum;
        this.retrievedAt = retrievedAt;
        this.sourceDate = sourceDate;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public long getPeriodId() { return periodId; }
    public boolean isKnown() { return known; }
    public BigDecimal getPrice() { return price; }
    public String getChecksum() { return checksum; }
    public Instant getRetrievedAt() { return retrievedAt; }
    public Instant getSourceDate() { return sourceDate; }
}
