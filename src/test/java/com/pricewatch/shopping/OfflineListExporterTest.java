package com.pricewatch.shopping;

import com.pricewatch.catalog.CatalogService;
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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OfflineListExporterTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private OfflineListExporter exporter;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;

    @BeforeEach
    void clearDatabase() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void exportIsSelfContainedAndListsKnownPrices() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        shopping.add("P000000229", 1);

        String html = new String(exporter.export(), StandardCharsets.UTF_8);

        assertTrue(html.contains("P000000124"));
        assertTrue(html.contains("P000000229"));
        assertTrue(html.contains("Prices saved"));
        assertTrue(html.contains("<style>"));
        assertTrue(html.contains("<script>"));
        assertFalse(html.contains("<link"));
        assertFalse(html.contains("src="));
    }

    @Test
    void exportWarnsWhenAnItemHasNoCurrentPrice() {
        catalog.importSample();
        shopping.mergeImported(java.util.List.of(
                new ShoppingService.ImportedItem("P000000124", 1, false, "NO-SUCH-RETAILER")));

        String html = new String(exporter.export(), StandardCharsets.UTF_8);

        assertTrue(html.contains("Total is incomplete"));
        assertTrue(html.contains("No current price"));
    }

}
