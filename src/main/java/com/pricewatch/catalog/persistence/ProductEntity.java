package com.pricewatch.catalog.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    @Column(length = 80, nullable = false)
    private String code;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "brand_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "brand_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "brand_zh_hans"))
    })
    private LocalizedLabel brand;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "name_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "name_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "name_zh_hans"))
    })
    private LocalizedLabel name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "category1_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "category1_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "category1_zh_hans"))
    })
    private LocalizedLabel category1;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "category2_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "category2_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "category2_zh_hans"))
    })
    private LocalizedLabel category2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "en", column = @Column(name = "category3_en")),
            @AttributeOverride(name = "zhHant", column = @Column(name = "category3_zh_hant")),
            @AttributeOverride(name = "zhHans", column = @Column(name = "category3_zh_hans"))
    })
    private LocalizedLabel category3;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<RetailerListingEntity> listings = new LinkedHashSet<>();

    protected ProductEntity() {
    }

    public ProductEntity(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public LocalizedLabel getBrand() { return brand; }
    public void setBrand(LocalizedLabel brand) { this.brand = brand; }
    public LocalizedLabel getName() { return name; }
    public void setName(LocalizedLabel name) { this.name = name; }
    public LocalizedLabel getCategory1() { return category1; }
    public void setCategory1(LocalizedLabel category1) { this.category1 = category1; }
    public LocalizedLabel getCategory2() { return category2; }
    public void setCategory2(LocalizedLabel category2) { this.category2 = category2; }
    public LocalizedLabel getCategory3() { return category3; }
    public void setCategory3(LocalizedLabel category3) { this.category3 = category3; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<RetailerListingEntity> getListings() { return listings; }

    public void addListing(RetailerListingEntity listing) {
        listings.add(listing);
    }
}
