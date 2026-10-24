package com.pricewatch.shopping;

import com.pricewatch.catalog.ProductView;

import java.math.BigDecimal;

public record ItemView(
        ProductView product,
        int quantity,
        boolean checked,
        BigDecimal unitPrice,
        String retailerCode,
        BigDecimal lineTotal,
        String selectedRetailerCode) {
}
