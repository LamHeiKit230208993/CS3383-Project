package com.pricewatch.catalog.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogImportException;
import com.pricewatch.catalog.LocalizedText;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class CatalogJsonParser {
    private static final int MAX_CODE_LENGTH = 80;
    private static final int MAX_LABEL_LENGTH = 255;
    private static final int MAX_OFFER_LENGTH = 1000;
    private static final int PRICE_PRECISION = 19;
    private static final int PRICE_SCALE = 4;

    private final ObjectMapper objectMapper;

    public CatalogJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ImportedCatalog parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new CatalogImportException("The price file is empty.");
        }

        try {
            JsonNode root;
            try (JsonParser jsonParser = objectMapper.getFactory().createParser(bytes)) {
                root = objectMapper.readTree(jsonParser);
                if (jsonParser.nextToken() != null) {
                    throw new CatalogImportException("The price file must contain one JSON document only.");
                }
            }
            if (root == null || !root.isArray() || root.isEmpty()) {
                throw new CatalogImportException("The price file must contain a non-empty product array.");
            }
            return parseProducts(root);
        } catch (CatalogImportException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new CatalogImportException("The price file is not valid JSON.", exception);
        }
    }

    private ImportedCatalog parseProducts(JsonNode root) {
        Map<String, ImportedCatalog.ImportedProduct> products = new LinkedHashMap<>();
        int listingCount = 0;
        int offerCount = 0;

        for (JsonNode node : root) {
            requireObject(node, "product");
            String code = requiredProductCode(node);
            ImportedCatalog.ImportedProduct product = parseProduct(code, node);
            ImportedCatalog.ImportedProduct previous = products.putIfAbsent(code, product);
            if (previous != null && !previous.equals(product)) {
                throw new CatalogImportException("The file contains conflicting records for product " + code + ".");
            }
            if (previous == null) {
                listingCount += product.listings().size();
                offerCount += product.listings().stream().mapToInt(listing -> listing.offers().size()).sum();
            }
        }

        return new ImportedCatalog(List.copyOf(products.values()), listingCount, offerCount);
    }

    private ImportedCatalog.ImportedProduct parseProduct(String code, JsonNode node) {
        LocalizedText brand = localized(node, "brand", MAX_LABEL_LENGTH, true);
        LocalizedText name = localized(node, "name", MAX_LABEL_LENGTH, true);
        LocalizedText category1 = localized(node, "cat1Name", MAX_LABEL_LENGTH, true);
        LocalizedText category2 = localized(node, "cat2Name", MAX_LABEL_LENGTH, true);
        LocalizedText category3 = localized(node, "cat3Name", MAX_LABEL_LENGTH, true);

        Map<String, BigDecimal> prices = parsePrices(node.get("prices"), code);
        Map<String, List<LocalizedText>> offers = parseOffers(node.get("offers"), code);
        Map<String, ImportedCatalog.ImportedListing> listings = new LinkedHashMap<>();

        prices.forEach((retailer, price) -> listings.put(retailer,
                new ImportedCatalog.ImportedListing(retailer, price, new ArrayList<>())));
        offers.forEach((retailer, texts) -> {
            ImportedCatalog.ImportedListing existing = listings.get(retailer);
            if (existing == null) {
                listings.put(retailer, new ImportedCatalog.ImportedListing(retailer, null, new ArrayList<>(texts)));
            } else {
                listings.put(retailer, new ImportedCatalog.ImportedListing(
                        retailer, existing.price(), new ArrayList<>(texts)));
            }
        });

        return new ImportedCatalog.ImportedProduct(code, brand, name, category1, category2, category3,
                List.copyOf(listings.values()));
    }

    private Map<String, BigDecimal> parsePrices(JsonNode node, String productCode) {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        if (node == null || node.isNull()) {
            return prices;
        }
        if (!node.isArray()) {
            throw new CatalogImportException("Prices for product " + productCode + " must be an array.");
        }
        for (JsonNode priceNode : node) {
            requireObject(priceNode, "price");
            String retailer = requiredText(priceNode, "supermarketCode", MAX_CODE_LENGTH, "retailer code");
            BigDecimal price = parsePrice(priceNode.get("price"), productCode, retailer);
            if (prices.containsKey(retailer) && !Objects.equals(prices.get(retailer), price)) {
                throw new CatalogImportException("The file contains conflicting prices for " + productCode
                        + " at " + retailer + ".");
            }
            prices.putIfAbsent(retailer, price);
        }
        return prices;
    }

    private Map<String, List<LocalizedText>> parseOffers(JsonNode node, String productCode) {
        Map<String, List<LocalizedText>> offers = new LinkedHashMap<>();
        if (node == null || node.isNull()) {
            return offers;
        }
        if (!node.isArray()) {
            throw new CatalogImportException("Offers for product " + productCode + " must be an array.");
        }
        for (JsonNode offerNode : node) {
            requireObject(offerNode, "offer");
            String retailer = requiredText(offerNode, "supermarketCode", MAX_CODE_LENGTH, "offer retailer code");
            LocalizedText text = localizedValues(offerNode, "offer", MAX_OFFER_LENGTH, false);
            offers.computeIfAbsent(retailer, ignored -> new ArrayList<>()).add(text);
        }
        return offers;
    }

    private BigDecimal parsePrice(JsonNode node, String productCode, String retailerCode) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual() && !node.isNumber()) {
            throw new CatalogImportException("The price for " + productCode + " at " + retailerCode
                    + " must be a decimal or blank.");
        }
        String raw = node.asText().trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(raw);
            if (amount.signum() < 0) {
                throw new CatalogImportException("Negative prices are not accepted for " + productCode + " at "
                        + retailerCode + ".");
            }
            BigDecimal scaled = amount.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
            if (scaled.precision() > PRICE_PRECISION) {
                throw new CatalogImportException("The price is too large for " + productCode + " at " + retailerCode + ".");
            }
            return scaled;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new CatalogImportException("The price for " + productCode + " at " + retailerCode
                    + " is not a supported decimal amount.", exception);
        }
    }

    private LocalizedText localized(JsonNode parent, String field, int maxLength, boolean required) {
        JsonNode label = parent.get(field);
        if (label == null || !label.isObject()) {
            if (!required && (label == null || label.isNull())) {
                return new LocalizedText("", "", "");
            }
            throw new CatalogImportException("The " + field + " labels must be an object.");
        }
        return localizedValues(label, field, maxLength, required);
    }

    private LocalizedText localizedValues(JsonNode label, String field, int maxLength, boolean required) {
        String en = localizedValue(label, "en", field, maxLength, required);
        String zhHant = localizedValue(label, "zh-Hant", field, maxLength, required);
        String zhHans = localizedValue(label, "zh-Hans", field, maxLength, required);
        return new LocalizedText(en, zhHant, zhHans);
    }

    private String localizedValue(JsonNode node, String language, String field, int maxLength, boolean required) {
        JsonNode value = node.get(language);
        if (value == null || value.isNull()) {
            if (!required) {
                return "";
            }
            throw new CatalogImportException("The " + field + " labels are missing " + language + ".");
        }
        if (!value.isTextual()) {
            throw new CatalogImportException("The " + field + " label for " + language + " must be text.");
        }
        String text = value.asText();
        if (required && text.isBlank()) {
            throw new CatalogImportException("The " + field + " label for " + language + " is blank.");
        }
        if (text.length() > maxLength) {
            throw new CatalogImportException("The " + field + " label for " + language + " is too long.");
        }
        return text;
    }

    private String requiredText(JsonNode node, String field, int maxLength, String description) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new CatalogImportException("A non-empty " + description + " is required.");
        }
        String text = value.asText();
        if (!text.equals(text.trim()) || text.length() > maxLength) {
            throw new CatalogImportException("The " + description + " is invalid.");
        }
        return text;
    }

    private String requiredProductCode(JsonNode node) {
        JsonNode value = node.get("code");
        if (value == null || value.isNull()) {
            throw new CatalogImportException("A non-empty product code is required.");
        }

        String code;
        if (value.isTextual()) {
            code = value.asText();
            if (code.isBlank()) {
                throw new CatalogImportException("A non-empty product code is required.");
            }
        } else if (value.isIntegralNumber() && value.bigIntegerValue().signum() >= 0) {
            code = value.asText();
        } else {
            throw new CatalogImportException("The product code must be text or a non-negative integer.");
        }

        if (!code.equals(code.trim()) || code.length() > MAX_CODE_LENGTH) {
            throw new CatalogImportException("The product code is invalid.");
        }
        return code;
    }

    private void requireObject(JsonNode node, String description) {
        if (node == null || !node.isObject()) {
            throw new CatalogImportException("Each " + description + " must be an object.");
        }
    }
}
