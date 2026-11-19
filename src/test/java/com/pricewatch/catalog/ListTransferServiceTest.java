package com.pricewatch.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.sharing.ExportFile;
import com.pricewatch.sharing.ListPreview;
import com.pricewatch.sharing.ListTransferService;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:share-service;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
class ListTransferServiceTest {
    @Autowired private ListTransferService transfer;
    @Autowired private ShoppingService shopping;
    @Autowired private CatalogService catalog;
    @Autowired private ObjectMapper mapper;
    @Autowired private ShoppingListRepository listRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void reset() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        catalog.importSample();
    }

    @Test
    void roundTripExportsVersionedSnapshotAndImportsSeparateList() throws Exception {
        shopping.add("P000000124", 2, "JASONS");
        shopping.update("P000000124", 2, true, "JASONS");

        ExportFile main = transfer.exportMainList();
        JsonNode root = mapper.readTree(main.bytes());
        assertEquals("pricewatch-list", root.path("format").asText());
        assertEquals(1, root.path("version").asInt());
        assertEquals("My list", root.path("name").asText());
        assertEquals("P000000124", root.path("items").get(0).path("productCode").asText());
        assertEquals(2, root.path("items").get(0).path("quantity").asInt());
        assertTrue(root.path("items").get(0).path("checked").asBoolean());
        assertEquals("JASONS", root.path("items").get(0).path("retailerCode").asText());

        ListPreview preview = transfer.importFile(main.bytes());

        assertEquals("My list (imported)", preview.name());
        assertEquals(2, transfer.lists().size());
        ItemView imported = preview.items().getFirst();
        assertEquals("P000000124", imported.product().code());
        assertEquals(2, imported.quantity());
        assertTrue(imported.checked());
        assertEquals("JASONS", imported.selectedRetailerCode());
        assertEquals(1, shopping.items().size());
        assertEquals(2, shopping.items().getFirst().quantity());
    }

    @Test
    void exportedImportedListCanBeExportedAgain() throws Exception {
        shopping.add("P000000124", 4);
        ListPreview imported = transfer.importFile(transfer.exportMainList().bytes());

        JsonNode root = mapper.readTree(transfer.export(imported.id()).bytes());

        assertEquals(imported.name(), root.path("name").asText());
        assertEquals(4, root.path("items").get(0).path("quantity").asInt());
        assertFalse(root.path("items").get(0).path("checked").asBoolean());
    }

    @Test
    void listedRetailerWithUnknownPriceKeepsItsChoice() {
        accept(product("P000000124", "[{\"supermarketCode\":\"JASONS\",\"price\":null}]"));

        ListPreview preview = transfer.importFile(file("Pinned",
                item("P000000124", 3, true, "JASONS")));

        ItemView imported = preview.items().getFirst();
        assertEquals("JASONS", imported.selectedRetailerCode());
        assertEquals("JASONS", imported.retailerCode());
        assertNull(imported.unitPrice());
        assertNull(imported.lineTotal());
    }

    @Test
    void blankNameGetsADefaultAndRepeatedImportsGetUniqueNames() {
        ListPreview first = transfer.importFile(file(" ", item("P000000124", 1, false, null)));
        ListPreview second = transfer.importFile(file("", item("P000000229", 1, false, null)));

        assertEquals("Shared list", first.name());
        assertEquals("Shared list (imported)", second.name());
        assertEquals(2, transfer.lists().size());
    }

    @Test
    void listsSummaryMarksTheMainListAndCountsItems() {
        shopping.add("P000000124", 1);
        transfer.importFile(file("Trip", item("P000000229", 2, false, null)));

        var lists = transfer.lists();

        assertEquals(2, lists.size());
        assertEquals("My list", lists.get(0).name());
        assertTrue(lists.get(0).mainList());
        assertEquals(1, lists.get(0).itemCount());
        assertEquals("Trip", lists.get(1).name());
        assertFalse(lists.get(1).mainList());
        assertEquals(1, lists.get(1).itemCount());
    }

    @Test
    void invalidFilesAreRejectedBeforeAnyWrite() {
        assertRejected("Choose a list file", new byte[0]);
        assertRejected("larger than 2 MB", new byte[ListTransferService.MAX_BYTES + 1]);
        assertRejected("not a readable list file", bytes("{not json"));
        assertRejected("not a pricewatch-list file",
                bytes("{\"format\":\"other\",\"version\":1,\"items\":[]}"));
        assertRejected("Only pricewatch-list version 1",
                bytes("{\"format\":\"pricewatch-list\",\"version\":2,\"items\":[]}"));
        assertRejected("Only pricewatch-list version 1",
                bytes("{\"format\":\"pricewatch-list\",\"version\":4294967297,"
                        + "\"items\":[{\"productCode\":\"P000000124\",\"quantity\":1,\"checked\":false}]}"));
        assertRejected("at least one item",
                bytes("{\"format\":\"pricewatch-list\",\"version\":1,\"items\":[]}"));
        assertRejected("at most 999 items", tooManyItems());
        assertRejected("appears more than once", file("Duplicate",
                item("P000000124", 1, false, null), item("P000000124", 2, false, null)));
        assertRejected("whole-number quantity", file("Zero", item("P000000124", 0, false, null)));
        assertRejected("whole-number quantity", file("Large", item("P000000124", 1000, false, null)));
        assertRejected("not in the accepted catalogue", file("Unknown",
                item("MISSING", 1, false, null)));
        assertRejected("true or false checked value", file("Checked",
                "{\"productCode\":\"P000000124\",\"quantity\":1,\"checked\":\"yes\"}"));
        assertRejected("true or false checked value", file("NoChecked",
                "{\"productCode\":\"P000000124\",\"quantity\":1}"));
        assertRejected("not a readable list file",
                bytes("{\"format\":\"pricewatch-list\",\"version\":1,\"items\":[\"x\"]}"));
        assertRejected("is not listed for product", file("MissingRetailer",
                item("P000000124", 1, false, "GONE")));
        assertTrue(transfer.lists().isEmpty());
    }

    @Test
    void rejectedImportLeavesTheExistingListUnchanged() {
        shopping.add("P000000124", 2);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile(file("Broken",
                        item("P000000124", 1, false, null), item("MISSING", 1, false, null))));

        assertTrue(error.getMessage().contains("not in the accepted catalogue"));
        assertEquals(1, transfer.lists().size());
        assertEquals(2, shopping.items().getFirst().quantity());
    }

    @Test
    void unknownListsAreReported() {
        assertThrows(IllegalArgumentException.class, () -> transfer.export(999));
        assertThrows(IllegalArgumentException.class, () -> transfer.preview(999));
    }

    private void assertRejected(String message, byte[] bytes) {
        long lists = listRepository.count();
        long items = itemRepository.count();
        long products = productRepository.count();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> transfer.importFile(bytes));

        assertTrue(error.getMessage().contains(message),
                () -> "Expected message containing '" + message + "' but was '" + error.getMessage() + "'");
        assertEquals(lists, listRepository.count());
        assertEquals(items, itemRepository.count());
        assertEquals(products, productRepository.count());
    }

    private byte[] file(String name, String... items) {
        String json = "{\"format\":\"pricewatch-list\",\"version\":1,"
                + "\"exportedAt\":\"2026-11-19T00:00:00Z\","
                + "\"name\":\"" + name + "\",\"items\":[" + String.join(",", items) + "]}";
        return bytes(json);
    }

    private String item(String code, int quantity, boolean checked, String retailer) {
        return "{\"productCode\":\"" + code + "\",\"quantity\":" + quantity
                + ",\"checked\":" + checked
                + ",\"retailerCode\":" + (retailer == null ? "null" : "\"" + retailer + "\"") + "}";
    }

    private byte[] tooManyItems() {
        String entry = item("P000000124", 1, false, null);
        StringBuilder json = new StringBuilder("{\"format\":\"pricewatch-list\",\"version\":1,"
                + "\"name\":\"Many\",\"items\":[");
        for (int index = 0; index < 1000; index++) {
            if (index > 0) json.append(',');
            json.append(entry);
        }
        return bytes(json.append("]}").toString());
    }

    private void accept(String... records) {
        catalog.accept(json(records), "SAMPLE", "test");
    }

    private byte[] json(String... records) {
        return bytes("[" + String.join(",", records) + "]");
    }

    private String product(String code, String prices) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":%s,"offers":[]}
                """.formatted(code, code, prices).trim();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
