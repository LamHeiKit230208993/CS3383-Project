package com.pricewatch.shopping;

import java.math.BigDecimal;
import java.util.List;

public record Summary(BigDecimal knownSubtotal, int unknownItemCount, int totalQuantity) {
    public static Summary from(List<ItemView> items) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(4);
        int unknownItems = 0;
        int quantity = 0;
        for (ItemView item : items) {
            quantity += item.quantity();
            if (item.lineTotal() == null) {
                unknownItems++;
            } else {
                subtotal = subtotal.add(item.lineTotal());
            }
        }
        return new Summary(subtotal, unknownItems, quantity);
    }
}
