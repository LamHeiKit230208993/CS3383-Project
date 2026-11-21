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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ListTransferServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private ListTransferService transfer;
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
    void exportThenImportRestoresProductsQuantitiesAndCheckedState() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        shopping.update("P000000124", 2, true, "JASONS");
        shopping.add("P000000229", 3);
        byte[] exported = transfer.export();

        clearDatabase();
        catalog.importSample();
        assertTrue(shopping.items().isEmpty());

        transfer.importFile(exported);

        var items = shopping.items();
        assertEquals(2, items.size());
        ItemView milk = items.stream().filter(i -> i.product().code().equals("P000000124")).findFirst().orElseThrow();
        assertEquals(2, milk.quantity());
        assertTrue(milk.checked());
        assertEquals("JASONS", milk.selectedRetailerCode());
        ItemView bakery = items.stream().filter(i -> i.product().code().equals("P000000229")).findFirst().orElseThrow();
        assertEquals(3, bakery.quantity());
        assertNull(bakery.selectedRetailerCode());
    }

    @Test
    void previewReadsTheFileWithoutWritingToList() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 4);
        byte[] exported = transfer.export();
        clearDatabase();
        catalog.importSample();

        var preview = transfer.preview(exported);

        assertEquals("My list", preview.listName());
        assertEquals(1, preview.itemCount());
        ListTransferService.PreviewItem item = preview.items().getFirst();
        assertEquals("P000000124", item.code());
        assertFalse(item.name().equals(item.code()), "preview should show the product name, not just the code");
        assertEquals(4, item.quantity());
        assertTrue(shopping.items().isEmpty());
    }

    @Test
    void malformedFileIsRejectedAndLeavesExistingListUnchanged() {
        catalog.importSample();
        shopping.add("P000000124", 2);

        assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile("not json".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile(("{\"format\":\"other\",\"items\":[]}").getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, shopping.items().size());
        assertEquals(2, shopping.items().getFirst().quantity());
    }

    @Test
    void invalidQuantityRejectsWholeImportWithoutPartialWrite() {
        catalog.importSample();
        shopping.add("P000000124", 5);
        String file = """
                {"format":"market-desk-list/1","listName":"Shared","exportedAt":"2026-11-15T10:00:00+08:00[Asia/Hong_Kong]",
                "items":[{"code":"P000000229","quantity":2,"checked":false,"retailerCode":null},
                         {"code":"P000000229","quantity":0,"checked":false,"retailerCode":null}]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile(file.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, shopping.items().size());
        assertEquals("P000000124", shopping.items().getFirst().product().code());
    }

    @Test
    void conflictingRetailerOnExistingItemRejectsWholeImport() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        String file = """
                {"format":"market-desk-list/1","listName":"Shared","exportedAt":"2026-11-15T10:00:00+08:00[Asia/Hong_Kong]",
                "items":[{"code":"P000000229","quantity":1,"checked":false,"retailerCode":null},
                         {"code":"P000000124","quantity":1,"checked":false,"retailerCode":"WELLCOME"}]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile(file.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, shopping.items().size());
        assertEquals("JASONS", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(2, shopping.items().getFirst().quantity());
    }

    @Test
    void unpricedRetailerIsKeptWhileAutomaticAndUnknownProductRejected() throws Exception {
        catalog.importSample();
        String kept = """
                {"format":"market-desk-list/1","listName":"Shared","exportedAt":"2026-11-15T10:00:00+08:00[Asia/Hong_Kong]",
                "items":[{"code":"P000000124","quantity":2,"checked":true,"retailerCode":"NOSUCH"}]}
                """;
        transfer.importFile(kept.getBytes(StandardCharsets.UTF_8));
        ItemView item = shopping.items().getFirst();
        assertEquals("NOSUCH", item.selectedRetailerCode());
        assertNull(item.unitPrice());
        assertTrue(item.checked());

        String unknownProduct = """
                {"format":"market-desk-list/1","listName":"Shared","exportedAt":"2026-11-15T10:00:00+08:00[Asia/Hong_Kong]",
                "items":[{"code":"MISSING-PRODUCT","quantity":1,"checked":false,"retailerCode":null}]}
                """;
        assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile(unknownProduct.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, shopping.items().size());
    }
}
