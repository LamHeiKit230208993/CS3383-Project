package com.pricewatch.tracking.persistence;

import com.pricewatch.catalog.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "wishlist_entries")
public class SavedProductEntity {
    @Id
    @Column(name = "product_code", length = 80)
    private String productCode;

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_code", nullable = false)
    private ProductEntity product;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    protected SavedProductEntity() {
    }

    public SavedProductEntity(ProductEntity product, Instant savedAt) {
        this.product = product;
        this.savedAt = savedAt;
    }

    public String getProductCode() { return productCode; }
    public ProductEntity getProduct() { return product; }
    public Instant getSavedAt() { return savedAt; }
}
