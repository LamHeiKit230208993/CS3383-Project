package com.pricewatch.catalog;

import com.pricewatch.catalog.importer.ConsumerCouncilSourceClient;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SearchRelevanceTest {
    @Autowired private CatalogService catalog;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;
    @MockitoBean private ConsumerCouncilSourceClient sourceClient;

    @BeforeEach
    void setUpCatalog() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        String json = "[" +
                product("NOODLE-NAME", "Instant noodle soup 70g", "Other",
                        "Pantry", "Instant noodles", "Food", "麵", "面") + "," +
                product("NAME-MATCH", "Mifun", "Other", "Pantry", "Dry food", "Food",
                        "米粉", "米粉") + "," +
                product("BRAND-MATCH", "Beef soup", "Mifun", "Pantry", "Dry food", "Food",
                        "牛肉湯", "牛肉汤") + "," +
                product("CAT3-MATCH", "Rice crackers", "Other", "Pantry", "Mifun products", "Food",
                        "米餅", "米饼") + "," +
                product("CAT2-MATCH", "Rice cakes", "Other", "Mifun", "Dry food", "Food",
                        "米糕", "米糕") + "," +
                product("DIM-SUM", "Hargow dim sum", "Other", "Frozen / Chilled processed food",
                        "Dim sum", "Noodles / Cooking needs", "蝦餃", "虾饺") + "]";
        catalog.accept(json.getBytes(StandardCharsets.UTF_8), "SAMPLE", "test");
    }

    @Test
    void productNameMatchesRankAheadOfBrandAndCategoriesAndBroadMatchesAreRelated() {
        SearchResults noodleResults = catalog.searchResults("noodle", "");

        assertEquals(List.of("NOODLE-NAME"), noodleResults.matches().stream().map(ProductView::code).toList());
        assertEquals(List.of("DIM-SUM"), noodleResults.related().stream().map(ProductView::code).toList());

        SearchResults ranked = catalog.searchResults("mifun", "");
        assertEquals(List.of("NAME-MATCH", "BRAND-MATCH", "CAT3-MATCH", "CAT2-MATCH"),
                ranked.matches().stream().map(ProductView::code).toList());
    }

    @Test
    void mixedProductNameAndBroadCategoryKeepsTheDirectMatch() {
        SearchResults results = catalog.searchResults("instant food", "");
        assertEquals(List.of("NOODLE-NAME"), results.matches().stream().map(ProductView::code).toList());
        assertTrue(results.related().isEmpty());
    }

    @Test
    void everyQueryTokenMustMatchAndSearchNormalizesAllThreeLanguages() {
        assertTrue(catalog.searchResults("instant missing", "").matches().isEmpty());
        assertEquals(List.of("NOODLE-NAME"), catalog.searchResults("ＮＯＯＤＬＥ", "")
                .matches().stream().map(ProductView::code).toList());

        String multilingual = "[" + product("MULTI", "Fresh noodles", "Brand", "Pantry", "Noodles",
                "Food", "鮮麵", "鲜面") + "]";
        catalog.accept(multilingual.getBytes(StandardCharsets.UTF_8), "SAMPLE", "test");
        assertEquals(List.of("MULTI"), catalog.search("鮮麵").stream().map(ProductView::code).toList());
        assertEquals(List.of("MULTI"), catalog.search("鲜面").stream().map(ProductView::code).toList());
    }

    @Test
    void categoryFilterUsesExactSourceCategoryAndReturnsAvailableOptions() {
        SearchResults filtered = catalog.searchResults("", "Pantry");

        assertEquals(List.of("Frozen / Chilled processed food", "Mifun", "Pantry"), filtered.categories());
        assertEquals(List.of("BRAND-MATCH", "CAT3-MATCH", "NAME-MATCH", "NOODLE-NAME"),
                filtered.matches().stream().map(ProductView::code).toList());
        assertTrue(filtered.related().isEmpty());
    }

    private String product(String code, String name, String brand, String category2, String category3,
                           String category1, String zhHant, String zhHans) {
        return """
                {"code":"%s","brand":{"en":"%s","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"%s","zh-Hans":"%s"},
                "cat1Name":{"en":"%s","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"%s","zh-Hant":"分類","zh-Hans":"分类"},
                "cat3Name":{"en":"%s","zh-Hant":"細類","zh-Hans":"细类"},"prices":[],"offers":[]}
                """.formatted(code, brand, name, zhHant, zhHans, category1, category2, category3).trim();
    }
}
