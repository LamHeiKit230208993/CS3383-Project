package com.pricewatch.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogueSearchTest {

    private static Catalogue sample() {
        return Fixtures.catalogue(
                Fixtures.product("P1", "Fresh Milk 236mL",
                        Fixtures.listing("AEON", "7.50")),
                Fixtures.product("P2", "Fresh Milk 946mL",
                        Fixtures.listing("AEON", "22.00")),
                Fixtures.product("P3", "Crustless Sandwich Bread",
                        Fixtures.listing("WELLCOME", "11.50")));
    }

    @Test
    void threeSourceNamesOfOneProductFindTheSameCode() {
        Product product = new Product("P1", Fixtures.name("Kowloon Dairy"),
                new LocalisedName("100 %Fresh Milk 236mL", "鮮牛奶 236毫升", "鲜牛奶 236毫升"),
                Fixtures.name("Dairy"), Fixtures.name("Milk"), Fixtures.name("Chilled"),
                List.of(Fixtures.listing("AEON", "7.50")));
        Catalogue catalogue = Fixtures.catalogue(product);

        assertThat(catalogue.find("Fresh Milk")).extracting(Product::getCode).containsExactly("P1");
        assertThat(catalogue.find("鮮牛奶")).extracting(Product::getCode).containsExactly("P1");
        assertThat(catalogue.find("鲜牛奶")).extracting(Product::getCode).containsExactly("P1");
    }

    @Test
    void searchMatchesBrandCategoryAndName() {
        Catalogue catalogue = sample();

        assertThat(catalogue.find("milk")).hasSize(2);
        assertThat(catalogue.find("bread")).extracting(Product::getCode).containsExactly("P3");
        assertThat(catalogue.find("cat2")).hasSize(3);
    }

    @Test
    void searchIsCaseInsensitiveAndTrims() {
        Catalogue catalogue = sample();

        assertThat(catalogue.find("  MILK  ")).hasSize(2);
    }

    @Test
    void blankQueryReturnsNothing() {
        Catalogue catalogue = sample();

        assertThat(catalogue.find("")).isEmpty();
        assertThat(catalogue.find(null)).isEmpty();
    }

    @Test
    void differentPackCodesAreNotMerged() {
        Catalogue catalogue = sample();

        List<String> codes = catalogue.find("milk").stream().map(Product::getCode).toList();

        assertThat(codes).containsExactlyInAnyOrder("P1", "P2");
    }

    @Test
    void unknownCodeReturnsNull() {
        assertThat(sample().product("NOPE")).isNull();
    }

    @Test
    void searchMatchesRetailers() {
        Catalogue catalogue = sample();

        assertThat(catalogue.find("AEON")).hasSize(2);
        assertThat(catalogue.find("wellcome")).extracting(Product::getCode).containsExactly("P3");
    }
}
