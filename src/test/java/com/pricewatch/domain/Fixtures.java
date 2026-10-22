package com.pricewatch.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class Fixtures {

    private Fixtures() {
    }

    static LocalisedName name(String text) {
        return new LocalisedName(text, text, text);
    }

    static Product product(String code, String name, Listing... listings) {
        return new Product(code, name("brand " + code), name(name),
                name("cat1"), name("cat2"), name("cat3"), List.of(listings));
    }

    static Listing listing(String retailer, String price) {
        return new Listing(retailer, price == null ? null : new BigDecimal(price), name("offer"));
    }

    static Catalogue catalogue(Product... products) {
        return new Catalogue(Instant.parse("2026-10-20T10:00:00Z"), LocalDate.parse("2026-10-19"), "test://sample", List.of(products));
    }
}
