package com.pricewatch.shopping;

import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;

import java.math.BigDecimal;

/**
 * The single rule for an item's effective unit price, shared by list totals and
 * the budget estimate. Automatic items use the current lowest known listing.
 * An explicit choice uses only that retailer's price. No silent switching.
 */
public final class EffectivePrice {
    private EffectivePrice() {
    }

    public static ListingView listingFor(ProductView product, String selectedRetailerCode) {
        if (selectedRetailerCode == null) {
            return product.lowestListings().stream().findFirst().orElse(null);
        }
        return product.listings().stream()
                .filter(listing -> listing.retailerCode().equals(selectedRetailerCode))
                .findFirst().orElse(null);
    }

    public static BigDecimal unitPrice(ProductView product, String selectedRetailerCode) {
        ListingView listing = listingFor(product, selectedRetailerCode);
        return listing == null ? null : listing.price();
    }

    public static String retailerCode(ProductView product, String selectedRetailerCode) {
        if (selectedRetailerCode != null) {
            return selectedRetailerCode;
        }
        ListingView listing = listingFor(product, null);
        return listing == null ? null : listing.retailerCode();
    }
}
