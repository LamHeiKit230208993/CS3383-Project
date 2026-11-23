package com.pricewatch.shopping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:list-transfer;DB_CLOSE_DELAY=-1")
@Transactional
class ShoppingListTransferTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private ProductRepository products;
    @Autowired private ObjectMapper mapper;

    @Test
    void namedListLifecycleKeepsMainCartAndPinnedRetailerIndependent() {
        catalog.importSample();
        shopping.add("P000000124", 1);
        long mainCartId = shopping.lists().getFirst().id();
        long weekendId = shopping.createList("  Weekend groceries  ");
        assertTrue(shopping.items(weekendId).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> shopping.createList("weekend groceries"));
        assertThrows(IllegalArgumentException.class, () -> shopping.createList("Cart"));
        assertThrows(IllegalArgumentException.class, () -> shopping.renameList(mainCartId, "Different"));
        assertThrows(IllegalArgumentException.class, () -> shopping.deleteList(mainCartId));

        shopping.add(weekendId, "P000000124", 2, "JASONS");
        shopping.add(weekendId, "P000000124", 1, null);
        shopping.renameList(weekendId, "Family groceries");
        assertEquals("Family groceries", shopping.exportList(weekendId).name());
        assertEquals(3, shopping.items(weekendId).getFirst().quantity());
        assertEquals("JASONS", shopping.items(weekendId).getFirst().selectedRetailerCode());
        assertEquals(1, shopping.items().getFirst().quantity());
        assertNull(shopping.items().getFirst().selectedRetailerCode());

        shopping.deleteList(weekendId);
        assertThrows(IllegalArgumentException.class, () -> shopping.items(weekendId));
        assertEquals(1, shopping.items().size());
        assertEquals(1, shopping.lists().size());
    }

    @Test
    void exportedListIsAnIndependentEditableCopy() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        shopping.add("P000000229", 3);
        shopping.update("P000000124", 2, true);
        var original = shopping.lists().getFirst();

        var transfer = shopping.exportList(original.id());
        var exportedJson = mapper.writeValueAsBytes(transfer);
        long copy = shopping.importList(new ByteArrayInputStream(exportedJson));
        long secondCopy = shopping.importList(new ByteArrayInputStream(exportedJson));

        assertEquals(1, transfer.version());
        assertEquals("My list", shopping.lists().getFirst().name());
        assertEquals("My list (2)", shopping.exportList(copy).name());
        assertEquals("My list (3)", shopping.exportList(secondCopy).name());
        assertEquals(transfer.items(), shopping.exportList(copy).items());
        assertEquals(new BigDecimal("12.5000"), shopping.items(copy).getFirst().unitPrice());

        shopping.update(copy, "P000000124", 4, false, "WELLCOME");
        shopping.remove(copy, "P000000229");
        assertEquals(1, shopping.items(copy).size());
        var edited = shopping.items(copy).getFirst();
        assertEquals(4, edited.quantity());
        assertFalse(edited.checked());
        assertEquals("WELLCOME", edited.selectedRetailerCode());
        assertEquals(transfer.items(), shopping.exportList(original.id()).items());
        assertEquals(transfer.items(), shopping.exportList(secondCopy).items());
    }

    @Test
    void absentProductBecomesInactiveAndSourceCatalogueCanResolveItLater() {
        long copy = importJson("""
                {"version":1,"name":"Groceries","items":[
                  {"productCode":"P000000124","displayName":"Shared milk","quantity":2,"checked":true,"retailerCode":"JASONS"}
                ]}
                """);

        var saved = shopping.items(copy).getFirst();
        assertEquals("Shared milk", saved.product().name().en());
        assertFalse(saved.product().active());
        assertTrue(products.findById("P000000124").orElseThrow().isImportedListReference());
        assertEquals("JASONS", saved.selectedRetailerCode());
        assertNull(saved.unitPrice());
        assertNull(saved.lineTotal());
        shopping.update(copy, "P000000124", 3, true, "JASONS");

        catalog.importSample();

        saved = shopping.items(copy).getFirst();
        assertTrue(saved.product().active());
        assertFalse(products.findById("P000000124").orElseThrow().isImportedListReference());
        assertEquals("100 %Fresh Milk 236mL", saved.product().name().en());
        assertEquals("JASONS", saved.selectedRetailerCode());
        assertEquals(3, saved.quantity());
        assertEquals(new BigDecimal("37.5000"), saved.lineTotal());
    }

    @Test
    void knownProductUsesLocalNameAndUnpricedPinDoesNotFallBack() {
        catalog.importSample();
        long copy = importJson("""
                {"version":1,"name":"Groceries","items":[
                  {"productCode":"P000000124","displayName":"Imported nickname","quantity":2,"checked":true,
                   "retailerCode":"OTHER","price":1}
                ]}
                """);

        var saved = shopping.items(copy).getFirst();
        assertEquals("100 %Fresh Milk 236mL", saved.product().name().en());
        assertTrue(saved.product().active());
        assertTrue(saved.checked());
        assertEquals("OTHER", saved.selectedRetailerCode());
        assertEquals("OTHER", saved.retailerCode());
        assertNull(saved.unitPrice());
        assertNull(saved.lineTotal());
    }

    @Test
    void invalidLaterRowsAndDamagedFilesLeaveExistingListsIntact() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        var before = shopping.exportList(null);
        String item = """
                {"productCode":"MISSING","displayName":"Unknown milk","quantity":2,"checked":false}
                """;
        String file = """
                {"version":1,"name":"Groceries","items":[%s,%s]}
                """;

        assertThrows(IllegalArgumentException.class, () -> importJson(file.formatted(item, item)));
        assertThrows(IllegalArgumentException.class, () -> importJson(file.formatted(item,
                item.replace("MISSING", "INVALID").replace("quantity\":2", "quantity\":0"))));
        assertThrows(IllegalArgumentException.class, () -> importJson(file.formatted(item, item).replace("version\":1", "version\":2")));
        assertThrows(IllegalArgumentException.class, () -> importJson("{\"version\":1,"));
        assertEquals(before, shopping.exportList(null));
        assertEquals(1, shopping.lists().size());
        assertFalse(products.existsById("MISSING"));
    }

    @Test
    void duplicateLongNameKeepsASurrogatePairIntact() {
        String name = "x".repeat(95) + "🛒abc";
        shopping.createList(name);
        long copy = importJson("""
                {"version":1,"name":"%s","items":[]}
                """.formatted(name));
        assertEquals("x".repeat(95) + " (2)", shopping.exportList(copy).name());
    }

    @Test
    void listNameValidationDoesNotChangeTheStoredList() {
        long list = shopping.createList("Weekend");
        for (String invalid : new String[]{null, " ", "x".repeat(101), "Bad\tName", "my list"}) {
            assertThrows(IllegalArgumentException.class, () -> shopping.renameList(list, invalid));
            assertEquals("Weekend", shopping.exportList(list).name());
        }
        long second = shopping.createList("Saturday");
        assertThrows(IllegalArgumentException.class, () -> shopping.renameList(list, "sAtUrDaY"));
        assertEquals("Saturday", shopping.exportList(second).name());
        shopping.renameList(list, "weekend");
        assertEquals("weekend", shopping.exportList(list).name());
    }

    @Test
    void inactiveCartReferenceCannotBeAddedThroughEitherCatalogueAction() {
        long copy = importJson("""
                {"version":1,"name":"Groceries","items":[
                  {"productCode":"MISSING","displayName":"Shared milk","quantity":2,"checked":false}
                ]}
                """);
        assertThrows(IllegalArgumentException.class, () -> shopping.add(copy, "MISSING", 1, null));
        assertEquals(2, shopping.items(copy).getFirst().quantity());
        var result = shopping.addMissingProducts(java.util.List.of("MISSING", "ABSENT", "MISSING"));
        assertEquals(0, result.added());
        assertEquals(2, result.unavailable());
        assertTrue(shopping.items().isEmpty());
    }

    private long importJson(String json) {
        return shopping.importList(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
