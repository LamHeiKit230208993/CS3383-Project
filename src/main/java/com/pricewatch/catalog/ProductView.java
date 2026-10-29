package com.pricewatch.catalog;

import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record ProductView(
        String code,
        LocalizedText brand,
        LocalizedText name,
        LocalizedText category1,
        LocalizedText category2,
        LocalizedText category3,
        boolean active,
        List<ListingView> listings,
        BigDecimal lowestPrice,
        List<ListingView> lowestListings) {
    public ProductView {
        listings = List.copyOf(listings);
        lowestListings = List.copyOf(lowestListings);
    }

    public static ProductView from(ProductEntity product) {
        List<ListingView> listings = product.getListings().stream()
                .sorted(Comparator.comparing(RetailerListingEntity::getRetailerCode))
                .map(listing -> new ListingView(listing.getRetailerCode(), listing.getPrice(),
                        listing.getOffers().stream().map(offer -> toText(offer.getText())).toList()))
                .toList();
        var lowest = listings.stream().map(ListingView::price).filter(price -> price != null)
                .min(java.math.BigDecimal::compareTo).orElse(null);
        List<ListingView> lowestListings = lowest == null ? List.of() : listings.stream()
                .filter(listing -> listing.price() != null && listing.price().compareTo(lowest) == 0)
                .toList();
        return new ProductView(product.getCode(), toText(product.getBrand()), toText(product.getName()),
                toText(product.getCategory1()), toText(product.getCategory2()), toText(product.getCategory3()),
                product.isActive(), listings, lowest, lowestListings);
    }

    private static LocalizedText toText(LocalizedLabel label) {
        return label == null ? new LocalizedText("", "", "")
                : new LocalizedText(label.getEn(), label.getZhHant(), label.getZhHans());
    }

}
