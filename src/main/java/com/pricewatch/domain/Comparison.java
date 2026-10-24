package com.pricewatch.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Comparison {

    private final Product product;
    private final List<Listing> cheapest;
    private final List<String> missing;
    private final Instant importedAt;

    public Comparison(Product product, List<Listing> listings, Instant importedAt) {
        this.product = product;
        this.importedAt = importedAt;

        List<Listing> priced = listings.stream().filter(Listing::hasPrice).toList();
        List<String> missingRetailers = new ArrayList<>();
        for (Listing listing : listings) {
            if (!listing.hasPrice()) {
                missingRetailers.add(listing.getRetailerCode());
            }
        }
        this.missing = List.copyOf(new LinkedHashSet<>(missingRetailers));

        if (priced.isEmpty()) {
            this.cheapest = List.of();
        } else {
            BigDecimal lowest = priced.stream()
                    .map(Listing::getPrice)
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
            Set<Listing> ties = new LinkedHashSet<>();
            for (Listing listing : priced) {
                if (listing.getPrice().compareTo(lowest) == 0) {
                    ties.add(listing);
                }
            }
            this.cheapest = List.copyOf(ties);
        }
    }

    public Product getProduct() {
        return product;
    }

    public List<Listing> getCheapest() {
        return cheapest;
    }

    public boolean hasWinner() {
        return !cheapest.isEmpty();
    }

    public boolean isTie() {
        return cheapest.size() > 1;
    }

    public List<String> getMissing() {
        return missing;
    }

    public List<Listing> getPriced() {
        return product.getListings().stream()
                .filter(Listing::hasPrice)
                .sorted(Comparator.comparing(Listing::getPrice))
                .toList();
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
