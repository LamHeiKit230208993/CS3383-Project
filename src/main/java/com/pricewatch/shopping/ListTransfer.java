package com.pricewatch.shopping;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Portable list data. Prices, internal IDs, and user settings stay local. */
public record ListTransfer(int version, String name, List<Item> items) {
    public static final int VERSION = 1;
    public static final int MAX_FILE_BYTES = 1024 * 1024;
    private static final int MAX_ITEMS = 1000;
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public ListTransfer {
        items = List.copyOf(items);
    }

    public record Item(String productCode, String displayName, int quantity, boolean checked,
                       String retailerCode) {}

    static ListTransfer forExport(String name, List<Item> items) {
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Lists over 1,000 items cannot be transferred in one file.");
        }
        var transfer = new ListTransfer(VERSION, name, items);
        try {
            if (JSON.writeValueAsBytes(transfer).length > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("This list is too large to transfer in one file.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not prepare the shopping list file.", exception);
        }
        return transfer;
    }

    static ListTransfer read(InputStream input) {
        final JsonNode root;
        try {
            byte[] file = input.readNBytes(MAX_FILE_BYTES + 1);
            if (file.length > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("Choose a list file no larger than 1 MB.");
            }
            root = JSON.readTree(file);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The uploaded file is not a valid shopping list JSON file.");
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("The uploaded file is not a shopping list.");
        }
        JsonNode version = root.path("version");
        if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != VERSION) {
            throw new IllegalArgumentException("This shopping list file version is unsupported.");
        }
        String name = requiredText(root, "name", 100);
        JsonNode rows = root.path("items");
        if (!rows.isArray() || rows.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("A shopping list file must contain at most 1,000 items.");
        }
        List<Item> parsed = new ArrayList<>();
        Set<String> productCodes = new HashSet<>();
        for (JsonNode row : rows) {
            String code = requiredText(row, "productCode", 80);
            if (!productCodes.add(code)) {
                throw new IllegalArgumentException("The shopping list file contains duplicate products.");
            }
            String label = requiredText(row, "displayName", 255);
            JsonNode quantity = row.path("quantity");
            JsonNode checked = row.path("checked");
            if (!quantity.isIntegralNumber() || !quantity.canConvertToInt()
                    || quantity.intValue() < 1 || quantity.intValue() > 999 || !checked.isBoolean()) {
                throw new IllegalArgumentException("Each list item needs a quantity from 1 to 999 and a checked state.");
            }
            JsonNode retailer = row.get("retailerCode");
            String codeOrNull = retailer == null || retailer.isNull()
                    ? null : requiredText(row, "retailerCode", 80);
            parsed.add(new Item(code, label, quantity.intValue(), checked.booleanValue(), codeOrNull));
        }
        return new ListTransfer(VERSION, name, parsed);
    }

    private static String requiredText(JsonNode object, String field, int maxLength) {
        JsonNode value = object.path(field);
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > maxLength
                || !value.textValue().equals(value.textValue().strip())
                || value.textValue().codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + field + " in the shopping list file.");
        }
        return value.textValue();
    }
}
