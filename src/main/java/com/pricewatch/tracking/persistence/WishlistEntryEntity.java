package com.pricewatch.tracking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * J05: a saved wishlist entry. Saving is separate from tracking (D-rules):
 * creating this row never starts observation collection.
 */
@Entity
@Table(name = "wishlist_entries")
public class WishlistEntryEntity {
    @Id
    @Column(length = 80, nullable = false)
    private String productCode;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    protected WishlistEntryEntity() {
    }

    public WishlistEntryEntity(String productCode, Instant savedAt) {
        this.productCode = productCode;
        this.savedAt = savedAt;
    }

    public String getProductCode() { return productCode; }
    public Instant getSavedAt() { return savedAt; }
}
