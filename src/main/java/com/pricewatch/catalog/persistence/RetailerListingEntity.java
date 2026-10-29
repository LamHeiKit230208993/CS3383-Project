package com.pricewatch.catalog.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retailer_listings", uniqueConstraints = @UniqueConstraint(
        name = "uk_listing_product_retailer", columnNames = {"product_code", "retailer_code"}))
public class RetailerListingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_code", nullable = false)
    private ProductEntity product;

    @Column(name = "retailer_code", nullable = false, length = 80)
    private String retailerCode;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferEntity> offers = new ArrayList<>();

    protected RetailerListingEntity() {
    }

    public RetailerListingEntity(ProductEntity product, String retailerCode, BigDecimal price) {
        this.product = product;
        this.retailerCode = retailerCode;
        this.price = price;
    }

    public void addOffer(OfferEntity offer) {
        offers.add(offer);
        offer.setListing(this);
    }

    public Long getId() { return id; }
    public ProductEntity getProduct() { return product; }
    public String getRetailerCode() { return retailerCode; }
    public BigDecimal getPrice() { return price; }
    public List<OfferEntity> getOffers() { return offers; }
}
