package com.pricewatch.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "listing")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_code", nullable = false, length = 32)
    private String retailerCode;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "offer_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "offer_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "offer_zh_hans"))
    })
    private LocalisedName offer;

    protected Listing() {
    }

    public Listing(String retailerCode, BigDecimal price, LocalisedName offer) {
        this.retailerCode = retailerCode;
        this.price = price;
        this.offer = offer == null ? new LocalisedName("", "", "") : offer;
    }

    public Long getId() {
        return id;
    }

    public String getRetailerCode() {
        return retailerCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean hasPrice() {
        return price != null;
    }

    public LocalisedName getOffer() {
        return offer;
    }

    public String offerText(String lang) {
        return offer.text(lang);
    }
}
