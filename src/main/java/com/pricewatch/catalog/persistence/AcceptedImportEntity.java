package com.pricewatch.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "accepted_import")
public class AcceptedImportEntity {
    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 16)
    private String origin;

    @Column(nullable = false, length = 500)
    private String sourceUrl;

    @Column(nullable = false)
    private Instant retrievedAt;

    private LocalDate sourceDate;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private int productCount;

    @Column(nullable = false)
    private int listingCount;

    @Column(nullable = false)
    private int offerCount;

    protected AcceptedImportEntity() {
    }

    public AcceptedImportEntity(String origin, String sourceUrl, Instant retrievedAt, LocalDate sourceDate,
                                String checksum, int productCount, int listingCount, int offerCount) {
        this.origin = origin;
        this.sourceUrl = sourceUrl;
        this.retrievedAt = retrievedAt;
        this.sourceDate = sourceDate;
        this.checksum = checksum;
        this.productCount = productCount;
        this.listingCount = listingCount;
        this.offerCount = offerCount;
    }

    public String getOrigin() { return origin; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getRetrievedAt() { return retrievedAt; }
    public LocalDate getSourceDate() { return sourceDate; }
    public String getChecksum() { return checksum; }
    public int getProductCount() { return productCount; }
    public int getListingCount() { return listingCount; }
    public int getOfferCount() { return offerCount; }
}
