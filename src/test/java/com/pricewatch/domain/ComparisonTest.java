package com.pricewatch.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonTest {

    @Test
    void lowestPriceWins() {
        Product product = Fixtures.product("P1", "milk",
                Fixtures.listing("AEON", "7.50"),
                Fixtures.listing("WELLCOME", "12.00"));

        Comparison comparison = product.compare(null);

        assertThat(comparison.hasWinner()).isTrue();
        assertThat(comparison.getCheapest()).singleElement()
                .extracting(Listing::getRetailerCode).isEqualTo("AEON");
        assertThat(comparison.isTie()).isFalse();
    }

    @Test
    void equalPricesAreTies() {
        Product product = Fixtures.product("P1", "bread",
                Fixtures.listing("AEON", "11.50"),
                Fixtures.listing("WELLCOME", "11.50"),
                Fixtures.listing("JASONS", "12.00"));

        Comparison comparison = product.compare(null);

        assertThat(comparison.isTie()).isTrue();
        assertThat(comparison.getCheapest()).extracting(Listing::getRetailerCode)
                .containsExactlyInAnyOrder("AEON", "WELLCOME");
    }

    @Test
    void missingPriceNeverWins() {
        Product product = Fixtures.product("P1", "laundry",
                Fixtures.listing("WATSONS", "65.00"),
                Fixtures.listing("PARKNSHOP", null));

        Comparison comparison = product.compare(null);

        assertThat(comparison.getCheapest()).singleElement()
                .extracting(Listing::getRetailerCode).isEqualTo("WATSONS");
        assertThat(comparison.getMissing()).containsExactly("PARKNSHOP");
        assertThat(comparison.getPriced()).hasSize(1);
    }

    @Test
    void allPricesMissingHasNoWinner() {
        Product product = Fixtures.product("P1", "oil",
                Fixtures.listing("AEON", null),
                Fixtures.listing("WELLCOME", null));

        Comparison comparison = product.compare(null);

        assertThat(comparison.hasWinner()).isFalse();
        assertThat(comparison.getMissing()).containsExactly("AEON", "WELLCOME");
    }
}
