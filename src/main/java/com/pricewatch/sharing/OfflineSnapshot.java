package com.pricewatch.sharing;

/** A self-contained, read-only HTML file for one saved list. */
public record OfflineSnapshot(String name, byte[] bytes) {
}
