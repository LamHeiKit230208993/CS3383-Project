package com.pricewatch.tracking;

import com.pricewatch.catalog.ProductView;

import java.time.Instant;

public record SavedProductView(ProductView product, Instant savedAt) {
}
