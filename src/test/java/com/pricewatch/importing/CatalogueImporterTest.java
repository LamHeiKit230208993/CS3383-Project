package com.pricewatch.importing;

import com.pricewatch.domain.Listing;
import com.pricewatch.domain.Product;
import com.pricewatch.store.CatalogueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CatalogueImporterTest {

    @Autowired
    private CatalogueImporter importer;

    @Autowired
    private CatalogueStore store;

    @BeforeEach
    void clean() {
        store.replace(new com.pricewatch.domain.Catalogue(java.time.Instant.now(), null, "test://clean", java.util.List.of()));
    }

    @Test
    void sampleImportsTheThreeProducts() {
        ImportResult result = importer.importSample();

        assertThat(result.accepted()).isTrue();
        assertThat(result.reason()).contains("3 products");
        assertThat(store.get().getProducts()).hasSize(3);
        assertThat(store.get().product("P000000124")).isNotNull();
        assertThat(store.get().product("P000000229")).isNotNull();
    }

    @Test
    void sampleKeepsThreeLanguageNamesOnOneCode() {
        importer.importSample();

        Product milk = store.get().product("P000000124");

        assertThat(milk.getName().text("en")).isEqualTo("100 %Fresh Milk 236mL");
        assertThat(milk.getName().text("zh-Hant")).isEqualTo("鮮牛奶 236毫升");
        assertThat(milk.getName().text("zh-Hans")).isEqualTo("鲜牛奶 236毫升");
    }

    @Test
    void differentPacksStaySeparateCodes() {
        importer.importSample();

        Product small = store.get().product("P000000124");
        Product large = store.get().product("P000000125");

        assertThat(small.getName().text("en")).isEqualTo("100 %Fresh Milk 236mL");
        assertThat(large.getName().text("en")).isEqualTo("100 %Fresh Milk 946mL");
    }

    @Test
    void offerTextIsKeptWhole() {
        importer.importSample();

        Listing aeon = store.get().product("P000000229").getListings().stream()
                .filter(listing -> listing.getRetailerCode().equals("AEON"))
                .findFirst().orElseThrow();

        assertThat(aeon.getPrice()).isEqualByComparingTo(new BigDecimal("11.50"));
        assertThat(aeon.getOffer().text("en")).isEqualTo("Buy 3 at $30.00");
    }

    @Test
    void retrievalTimeIsRecordedButSourceDateIsUnknown() {
        importer.importSample();

        assertThat(store.get().getImportedAt()).isNotNull();
        assertThat(store.get().getSourceDate()).isNull();
    }

    @Test
    void repeatedImportDoesNotDuplicateListings() {
        importer.importSample();
        int firstCount = store.get().product("P000000229").getListings().size();

        importer.importSample();

        assertThat(store.get().getProducts()).hasSize(3);
        assertThat(store.get().product("P000000229").getListings()).hasSize(firstCount);
    }

    @Test
    void invalidJsonKeepsLastAcceptedCatalogue() {
        importer.importSample();

        ImportResult result = importer.importJson("{not json".getBytes(StandardCharsets.UTF_8), "test://bad");

        assertThat(result.accepted()).isFalse();
        assertThat(store.get().getProducts()).hasSize(3);
    }

    @Test
    void emptyArrayKeepsLastAcceptedCatalogue() {
        importer.importSample();

        ImportResult result = importer.importJson("[]".getBytes(StandardCharsets.UTF_8), "test://empty");

        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).contains("no products");
        assertThat(store.get().getProducts()).hasSize(3);
    }

    @Test
    void productWithoutThreeLabelsIsRejected() {
        importer.importSample();
        String json = """
                [{"code":"P1","brand":{"en":"B","zh-Hant":"b","zh-Hans":"b"},
                  "name":{"en":"Name only"},
                  "cat1Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat2Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat3Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "prices":[],"offers":[]}]
                """;

        ImportResult result = importer.importJson(json.getBytes(StandardCharsets.UTF_8), "test://missing-label");

        assertThat(result.accepted()).isFalse();
        assertThat(store.get().getProducts()).hasSize(3);
    }

    @Test
    void duplicateCodeIsRejected() {
        importer.importSample();
        String json = """
                [{"code":"P1","brand":{"en":"B","zh-Hant":"b","zh-Hans":"b"},
                  "name":{"en":"N","zh-Hant":"n","zh-Hans":"n"},
                  "cat1Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat2Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat3Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "prices":[],"offers":[]},
                 {"code":"P1","brand":{"en":"B","zh-Hant":"b","zh-Hans":"b"},
                  "name":{"en":"N","zh-Hant":"n","zh-Hans":"n"},
                  "cat1Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat2Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat3Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "prices":[],"offers":[]}]
                """;

        ImportResult result = importer.importJson(json.getBytes(StandardCharsets.UTF_8), "test://duplicate");

        assertThat(result.accepted()).isFalse();
        assertThat(result.reason()).contains("Duplicate product code");
    }

    @Test
    void unparseablePriceStaysUnknownNotZero() {
        String json = """
                [{"code":"P1","brand":{"en":"B","zh-Hant":"b","zh-Hans":"b"},
                  "name":{"en":"N","zh-Hant":"n","zh-Hans":"n"},
                  "cat1Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat2Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat3Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "prices":[{"supermarketCode":"AEON","price":"ask"}],"offers":[]}]
                """;

        importer.importJson(json.getBytes(StandardCharsets.UTF_8), "test://bad-price");

        Listing listing = store.get().product("P1").getListings().get(0);
        assertThat(listing.getPrice()).isNull();
        assertThat(listing.getPrice()).isNotEqualTo(BigDecimal.ZERO);
    }

    @Test
    void missingPriceStringStaysUnknown() {
        String json = """
                [{"code":"P1","brand":{"en":"B","zh-Hant":"b","zh-Hans":"b"},
                  "name":{"en":"N","zh-Hant":"n","zh-Hans":"n"},
                  "cat1Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat2Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "cat3Name":{"en":"c","zh-Hant":"c","zh-Hans":"c"},
                  "prices":[{"supermarketCode":"AEON"}],"offers":[]}]
                """;

        importer.importJson(json.getBytes(StandardCharsets.UTF_8), "test://no-price");

        assertThat(store.get().product("P1").getListings().get(0).hasPrice()).isFalse();
    }
}
