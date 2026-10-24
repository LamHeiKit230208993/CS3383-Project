package com.pricewatch.catalog.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "retailer_offers")
public class OfferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private RetailerListingEntity listing;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "offer_en", length = 1000)),
            @AttributeOverride(name = "zhHant", column = @Column(name = "offer_zh_hant", length = 1000)),
            @AttributeOverride(name = "zhHans", column = @Column(name = "offer_zh_hans", length = 1000))
    })
    private LocalizedLabel text;

    protected OfferEntity() {
    }

    public OfferEntity(LocalizedLabel text) {
        this.text = text;
    }

    public void setListing(RetailerListingEntity listing) { this.listing = listing; }
    public LocalizedLabel getText() { return text; }
}
