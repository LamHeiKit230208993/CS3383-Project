package com.pricewatch.tracking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "wishlist_entries")
public class WishlistEntryEntity {
    @Id
    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WishlistEntryEntity() {
    }

    public WishlistEntryEntity(String productCode, Instant createdAt) {
        this.productCode = productCode;
        this.createdAt = createdAt;
    }

    public String getProductCode() { return productCode; }
    public Instant getCreatedAt() { return createdAt; }
}
