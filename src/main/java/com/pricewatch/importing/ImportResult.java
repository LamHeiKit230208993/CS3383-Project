package com.pricewatch.importing;

import com.pricewatch.domain.Catalogue;

public record ImportResult(boolean accepted, String reason, int productCount, Catalogue catalogue) {

    public static ImportResult accepted(Catalogue catalogue) {
        return new ImportResult(true, "Accepted " + catalogue.getProducts().size() + " products",
                catalogue.getProducts().size(), catalogue);
    }

    public static ImportResult rejected(String reason) {
        return new ImportResult(false, reason, 0, null);
    }
}
