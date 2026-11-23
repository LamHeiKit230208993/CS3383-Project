package com.pricewatch.shopping;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListTransferValidationTest {
    @ParameterizedTest
    @MethodSource("invalidFiles")
    void rejectsEachInvalidInputAtTheExpectedValidationStep(String json, String message) {
        var failure = assertThrows(IllegalArgumentException.class, () -> read(json));
        assertEquals(message, failure.getMessage());
    }

    static Stream<Arguments> invalidFiles() {
        String badVersion = "This shopping list file version is unsupported.";
        String badItem = "Each list item needs a quantity from 1 to 999 and a checked state.";
        return Stream.of(
                Arguments.of("", "The uploaded file is not a shopping list."),
                Arguments.of("[]", "The uploaded file is not a shopping list."),
                Arguments.of("{\"version\":1,\"version\":1}", "The uploaded file is not a valid shopping list JSON file."),
                Arguments.of("{\"version\":1} {}", "The uploaded file is not a valid shopping list JSON file."),
                Arguments.of("{\"version\":\"1\"}", badVersion),
                Arguments.of("{\"version\":2147483648}", badVersion),
                Arguments.of("{\"version\":1,\"name\":\"Valid\",\"items\":{}}", "A shopping list file must contain at most 1,000 items."),
                Arguments.of("{\"version\":1,\"name\":1}", "Invalid name in the shopping list file."),
                Arguments.of("{\"version\":1,\"name\":\"   \"}", "Invalid name in the shopping list file."),
                Arguments.of("{\"version\":1,\"name\":\"" + "x".repeat(101) + "\"}", "Invalid name in the shopping list file."),
                Arguments.of("{\"version\":1,\"name\":\" leading\"}", "Invalid name in the shopping list file."),
                Arguments.of("{\"version\":1,\"name\":\"before\\tafter\"}", "Invalid name in the shopping list file."),
                Arguments.of(fileWithOneRow("2.5", "true", ""), badItem),
                Arguments.of(fileWithOneRow("2147483648", "true", ""), badItem),
                Arguments.of(fileWithOneRow("1000", "true", ""), badItem),
                Arguments.of(fileWithOneRow("1", "\"true\"", ""), badItem));
    }

    @Test
    void acceptsQuantityBoundariesAndExplicitNullRetailer() {
        var first = read(fileWithOneRow("1", "false", ",\"retailerCode\":null")).items().getFirst();
        var last = read(fileWithOneRow("999", "true", ",\"retailerCode\":\"JASONS\"")).items().getFirst();

        assertEquals(new ListTransfer.Item("P1", "Milk", 1, false, null), first);
        assertNull(first.retailerCode());
        assertEquals(new ListTransfer.Item("P1", "Milk", 999, true, "JASONS"), last);
    }

    @Test
    void itemLimitIsEnforcedOnImportAndExport() {
        String file = "{\"version\":1,\"name\":\"Groceries\",\"items\":[%s]}";
        assertEquals(1000, read(file.formatted(rows(1000))).items().size());
        assertEquals("A shopping list file must contain at most 1,000 items.",
                assertThrows(IllegalArgumentException.class, () -> read(file.formatted(rows(1001)))).getMessage());

        var item = new ListTransfer.Item("P1", "Milk", 1, false, null);
        assertEquals(1000, ListTransfer.forExport("Groceries", Collections.nCopies(1000, item)).items().size());
        assertEquals("Lists over 1,000 items cannot be transferred in one file.",
                assertThrows(IllegalArgumentException.class,
                        () -> ListTransfer.forExport("Groceries", Collections.nCopies(1001, item))).getMessage());
    }

    @Test
    void byteLimitIsEnforcedOnImportAndExport() {
        String valid = "{\"version\":1,\"name\":\"Groceries\",\"items\":[]}";
        String atLimit = valid + " ".repeat(ListTransfer.MAX_FILE_BYTES - valid.length());
        assertEquals("Groceries", read(atLimit).name());
        assertEquals("Choose a list file no larger than 1 MB.",
                assertThrows(IllegalArgumentException.class, () -> read(atLimit + " ")).getMessage());

        // Multibyte labels and identifiers can exceed the byte limit below the row limit.
        var items = IntStream.range(0, 1000).mapToObj(i -> new ListTransfer.Item(
                "商".repeat(75) + i, "鮮".repeat(255), 1, false, "店".repeat(80))).toList();
        assertEquals("This list is too large to transfer in one file.",
                assertThrows(IllegalArgumentException.class,
                        () -> ListTransfer.forExport("Groceries", items)).getMessage());
    }

    private static String fileWithOneRow(String quantity, String checked, String fields) {
        return """
                {"version":1,"name":"Groceries","items":[
                  {"productCode":"P1","displayName":"Milk","quantity":%s,"checked":%s%s}
                ]}
                """.formatted(quantity, checked, fields);
    }

    private static String rows(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "{\"productCode\":\"P" + i + "\",\"displayName\":\"Milk\",\"quantity\":1,\"checked\":false}")
                .collect(Collectors.joining(","));
    }

    private static ListTransfer read(String json) {
        return ListTransfer.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
