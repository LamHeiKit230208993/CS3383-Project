package com.pricewatch.ask;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The only product and price values the model may use. Every value comes from
 * the accepted catalogue; a null price means unknown, never out of stock.
 */
public record AskFacts(
        String productCode,
        String productName,
        String brand,
        String category,
        List<PriceFact> prices,
        BigDecimal lowestPrice,
        List<String> lowestRetailers,
        Instant retrievedAt,
        String question) {

    public AskFacts {
        prices = List.copyOf(prices);
        lowestRetailers = List.copyOf(lowestRetailers);
    }

    public record PriceFact(String retailerCode, BigDecimal price) {
    }

    public List<BigDecimal> knownPrices() {
        return prices.stream().map(PriceFact::price).filter(price -> price != null).toList();
    }

    public Map<String, Object> asMap() {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("productCode", productCode);
        facts.put("productName", productName);
        facts.put("brand", brand);
        facts.put("category", category);
        facts.put("retailers", prices.stream()
                .map(price -> {
                    Map<String, Object> retailer = new LinkedHashMap<>();
                    retailer.put("retailerCode", price.retailerCode());
                    retailer.put("price", price.price() == null ? null : price.price().toPlainString());
                    return retailer;
                })
                .toList());
        facts.put("lowestPrice", lowestPrice == null ? null : lowestPrice.toPlainString());
        facts.put("lowestRetailers", lowestRetailers);
        facts.put("retrievedAt", retrievedAt == null ? null : retrievedAt.toString());
        facts.put("sourcePriceDate", "unknown");
        facts.put("question", question);
        return facts;
    }
}
