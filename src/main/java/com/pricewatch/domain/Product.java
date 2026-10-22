package com.pricewatch.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "brand_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "brand_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "brand_zh_hans"))
    })
    private LocalisedName brand;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "name_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "name_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "name_zh_hans"))
    })
    private LocalisedName name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "cat1_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "cat1_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "cat1_zh_hans"))
    })
    private LocalisedName category1;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "cat2_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "cat2_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "cat2_zh_hans"))
    })
    private LocalisedName category2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "cat3_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "cat3_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "cat3_zh_hans"))
    })
    private LocalisedName category3;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_code", nullable = false)
    private List<Listing> listings = new ArrayList<>();

    protected Product() {
    }

    public Product(String code, LocalisedName brand, LocalisedName name,
                   LocalisedName category1, LocalisedName category2, LocalisedName category3,
                   List<Listing> listings) {
        this.code = code;
        this.brand = brand;
        this.name = name;
        this.category1 = category1;
        this.category2 = category2;
        this.category3 = category3;
        this.listings = new ArrayList<>(listings);
    }

    public String getCode() {
        return code;
    }

    public LocalisedName getBrand() {
        return brand;
    }

    public LocalisedName getName() {
        return name;
    }

    public LocalisedName getCategory1() {
        return category1;
    }

    public LocalisedName getCategory2() {
        return category2;
    }

    public LocalisedName getCategory3() {
        return category3;
    }

    public List<Listing> getListings() {
        return listings;
    }

    public List<String> searchableText() {
        List<String> text = new ArrayList<>();
        text.addAll(brand.all());
        text.addAll(name.all());
        text.addAll(category1.all());
        text.addAll(category2.all());
        text.addAll(category3.all());
        return text;
    }

    public Comparison compare(java.time.Instant importedAt) {
        return new Comparison(this, listings, importedAt);
    }
}
