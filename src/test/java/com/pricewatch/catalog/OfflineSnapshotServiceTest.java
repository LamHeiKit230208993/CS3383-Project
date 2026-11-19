package com.pricewatch.catalog;

import com.pricewatch.sharing.ListTransferService;
import com.pricewatch.sharing.OfflineSnapshot;
import com.pricewatch.sharing.OfflineSnapshotService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:offline-snapshot;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
class OfflineSnapshotServiceTest {
    private static final Instant SAVED_AT = Instant.parse("2026-11-19T04:00:00Z");

    @Autowired private OfflineSnapshotService snapshots;
    @Autowired private ShoppingService shopping;
    @Autowired private CatalogService catalog;
    @Autowired private ListTransferService transfer;
    @Autowired private ShoppingListRepository listRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @MockitoBean private Clock appClock;

    @BeforeEach
    void reset() {
        when(appClock.instant()).thenReturn(SAVED_AT);
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        catalog.importSample();
    }

    @Test
    void snapshotIsSelfContainedAndShowsSavedAmountsAndAge() {
        shopping.add("P000000124", 2, "JASONS");
        shopping.update("P000000124", 2, true, "JASONS");

        OfflineSnapshot snapshot = snapshots.snapshot(null, Locale.ENGLISH);
        String html = text(snapshot);

        assertEquals("My list", snapshot.name());
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("100 %Fresh Milk 236mL"));
        assertTrue(html.contains("Kowloon Dairy"));
        assertTrue(html.contains(">P000000124</code>"));
        assertTrue(html.contains("data-label=\"Packs\">2</td>"));
        assertTrue(html.contains("data-label=\"Checked\">Yes</td>"));
        assertTrue(html.contains("JASONS"));
        assertTrue(html.contains("HK$12.50"));
        assertTrue(html.contains("HK$25.00"));
        assertTrue(html.contains("<time datetime=\"2026-11-19T04:00:00Z\">19 Nov 2026, 12:00 HKT</time>"));
        assertTrue(html.contains("Data age"));
        assertTrue(html.contains("Source price date"));
        assertTrue(html.contains("Unknown"));
        assertTrue(html.contains("Read-only"));
        assertTrue(html.contains("No connection needed"));

        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<form"));
        assertFalse(html.contains("<a "));
        assertFalse(html.contains("href="));
        assertFalse(html.contains("src="));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));
        assertFalse(html.contains("url("));
    }

    @Test
    void localizedSnapshotUsesSceneTextAndSourceProductLabels() {
        shopping.add("P000000124", 1, "JASONS");

        String html = text(snapshots.snapshot(null, Locale.forLanguageTag("zh-HK")));

        assertTrue(html.contains("<html lang=\"zh-Hant\">"));
        assertTrue(html.contains("唯讀快照"));
        assertTrue(html.contains("鮮牛奶 236毫升"));
        assertTrue(html.contains("維記"));
        assertTrue(html.contains("已核對"));
        assertTrue(html.contains("lang=\"zh-Hant\""));
        assertTrue(html.contains("HK$12.50"));

        String simplified = text(snapshots.snapshot(null, Locale.forLanguageTag("zh-CN")));
        assertTrue(simplified.contains("<html lang=\"zh-Hans\">"));
        assertTrue(simplified.contains("只读快照"));
        assertTrue(simplified.contains("鲜牛奶 236毫升"));
        assertTrue(simplified.contains("维记"));
    }

    @Test
    void automaticChoiceNamesTheRetailerUsedForTheSavedPrice() {
        shopping.add("P000000124", 1);

        String html = text(snapshots.snapshot(null, Locale.ENGLISH));

        assertTrue(html.contains("AEON (automatic)"));
        assertTrue(html.contains("HK$7.50"));
    }

    @Test
    void selectedRetailerWithNoCurrentPriceKeepsItsChoiceAndUnknownAmounts() {
        accept(product("P000000124", "[{\"supermarketCode\":\"JASONS\",\"price\":null}]"));
        long listId = transfer.importFile(file("Pinned",
                item("P000000124", 3, true, "JASONS"))).id();

        String html = text(snapshots.snapshot(listId, Locale.ENGLISH));

        assertTrue(html.contains("JASONS"));
        assertTrue(html.contains("data-label=\"Checked\">Yes</td>"));
        assertTrue(html.contains("data-label=\"Unit price\">Unknown</td>"));
        assertTrue(html.contains("data-label=\"Line amount\">Unknown</td>"));
        assertEquals(0, count(html, "HK$"));
    }

    @Test
    void listNameAndProductLabelsAreEscapedAndEmptyListsStayReadable() {
        long listId = transfer.importFile(file("<b>Trip</b>",
                item("P000000124", 1, false, null))).id();
        transfer.importFile(file("<i>Empty</i>", item("P000000229", 1, false, null)));

        String named = text(snapshots.snapshot(listId, Locale.ENGLISH));
        assertTrue(named.contains("&lt;b&gt;Trip&lt;/b&gt;"));
        assertFalse(named.contains("<b>Trip</b>"));

        String empty = text(snapshots.snapshot(null, Locale.ENGLISH));
        assertTrue(empty.contains("This list had no items when it was saved."));
        assertTrue(empty.contains("0 products"));
    }

    @Test
    void unknownListIsReported() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> snapshots.snapshot(999L, Locale.ENGLISH));
        assertTrue(error.getMessage().contains("That list was not found."));
    }

    private static String text(OfflineSnapshot snapshot) {
        return new String(snapshot.bytes(), StandardCharsets.UTF_8);
    }

    private static int count(String value, String needle) {
        int found = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            found++;
            index += needle.length();
        }
        return found;
    }

    private byte[] file(String name, String... items) {
        String json = "{\"format\":\"pricewatch-list\",\"version\":1,"
                + "\"name\":\"" + name + "\",\"items\":[" + String.join(",", items) + "]}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String item(String code, int quantity, boolean checked, String retailer) {
        return "{\"productCode\":\"" + code + "\",\"quantity\":" + quantity
                + ",\"checked\":" + checked
                + ",\"retailerCode\":" + (retailer == null ? "null" : "\"" + retailer + "\"") + "}";
    }

    private void accept(String... records) {
        catalog.accept(json(records), "SAMPLE", "test");
    }

    private byte[] json(String... records) {
        return ("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8);
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
}
