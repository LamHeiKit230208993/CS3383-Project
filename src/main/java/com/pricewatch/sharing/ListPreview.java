package com.pricewatch.sharing;

import com.pricewatch.shopping.ItemView;

import java.util.List;

/** A saved list shown after import, with its own list id and unchanged records. */
public record ListPreview(Long id, String name, List<ItemView> items) {
    public ListPreview {
        items = List.copyOf(items);
    }
}
