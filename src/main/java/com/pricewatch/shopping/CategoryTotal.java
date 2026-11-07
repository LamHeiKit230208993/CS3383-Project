package com.pricewatch.shopping;

import java.math.BigDecimal;

/** J07: planned known cost for one category (category2, fallback "Other"). */
public record CategoryTotal(String category, BigDecimal knownSubtotal, int itemCount) {
}
