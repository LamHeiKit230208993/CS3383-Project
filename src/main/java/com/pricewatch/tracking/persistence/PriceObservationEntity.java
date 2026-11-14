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

@Entity
@Table(name = "price_observations", uniqueConstraints = @UniqueConstraint(
        name = "uk_observation_product_retailer_checksum",
        columnNames = {"product_code", "retailer_code", "checksum"}))
public class PriceObservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "retailer_code", length = 80, nullable = false)
    private String retailerCode;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "offer_snapshot", length = 2000)
    private String offerSnapshot;

    protected PriceObservationEntity() {
    }

    public PriceObservationEntity(String productCode, String retailerCode, BigDecimal price,
                                  Instant retrievedAt, String checksum, String offerSnapshot) {
        this.productCode = productCode;
        this.retailerCode = retailerCode;
        this.price = price;
        this.retrievedAt = retrievedAt;
        this.checksum = checksum;
        this.offerSnapshot = offerSnapshot;
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRetailerCode() { return retailerCode; }
    public BigDecimal getPrice() { return price; }
    public Instant getRetrievedAt() { return retrievedAt; }
    public String getChecksum() { return checksum; }
    public String getOfferSnapshot() { return offerSnapshot; }
}
