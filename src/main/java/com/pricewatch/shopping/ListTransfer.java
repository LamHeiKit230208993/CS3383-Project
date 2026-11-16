package com.pricewatch.shopping;

import java.util.List;

/** The J10 export/import file: one chosen list plus a format version and export time. */
public record ListTransfer(String format, String listName, String exportedAt, List<ListTransferItem> items) {
    public static final String FORMAT = "market-desk-list/1";

    public ListTransfer {
        items = List.copyOf(items);
    }

    public record ListTransferItem(String code, int quantity, boolean checked, String retailerCode) {
    }
}
