package com.pricewatch.tracking;

import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoHistoryService {
    public static final String DEMO_SOURCE = "demo://synthetic-price";
    private static final String CHECKSUM = "demo";
    private static final List<Integer> DEMO_DAYS = List.of(21, 20, 18, 15, 14, 8, 7, 1);

    private final CatalogReplacementService replacementService;
    private final TrackingRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final TrackingService trackingService;
    private final AlertService alertService;
    private final Clock clock;

    public DemoHistoryService(CatalogReplacementService replacementService,
                              TrackingRepository trackingRepository,
                              PriceObservationRepository observationRepository,
                              ProductRepository productRepository,
                              TrackingService trackingService,
                              AlertService alertService,
                              Clock clock) {
        this.replacementService = replacementService;
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.trackingService = trackingService;
        this.alertService = alertService;
        this.clock = clock;
    }

    @Transactional
    public DemoResult seed() {
        ImportedCatalog catalog = demoCatalog();
        replacementService.replace(catalog, "DEMO", DEMO_SOURCE, clock.instant(), CHECKSUM);
        return recordDemoHistory(catalog);
    }

    public record DemoResult(int productCount, int observationCount) {
    }

    private DemoResult recordDemoHistory(ImportedCatalog catalog) {
        Instant now = clock.instant();
        Map<String, TrackingEntity> periods = new LinkedHashMap<>();
        for (ImportedCatalog.ImportedProduct product : catalog.products()) {
            ProductEntity entity = productRepository.findWithListingsByCode(product.code()).orElse(null);
            if (entity == null) continue;
            for (ImportedCatalog.ImportedListing listing : product.listings()) {
                TrackingEntity period = trackingRepository
                        .findByProduct_CodeAndRetailerCodeAndEnabledTrue(product.code(), listing.retailerCode())
                        .orElseGet(() -> trackingRepository.saveAndFlush(new TrackingEntity(
                                entity, listing.retailerCode(), now.minus(Duration.ofDays(DEMO_DAYS.getFirst())))));
                periods.put(product.code() + "|" + listing.retailerCode(), period);
            }
            // Keep the backdated periods when saving the product to Wishlist.
            trackingService.save(product.code());
        }

        TrackingEntity milkAtAeon = periods.get("DEMO-MILK|AEON");
        if (milkAtAeon != null) milkAtAeon.setTargetPrice(new BigDecimal("9.00"));

        int recorded = 0;
        for (Map.Entry<String, List<BigDecimal>> series : demoSeries().entrySet()) {
            TrackingEntity period = periods.get(series.getKey());
            if (period == null) continue;
            PriceObservationEntity previous = null;
            for (int index = 0; index < DEMO_DAYS.size(); index++) {
                Instant observedAt = now.minus(Duration.ofDays(DEMO_DAYS.get(index)));
                PriceObservationEntity observation = observationRepository.saveAndFlush(
                        new PriceObservationEntity(period, series.getValue().get(index), observedAt,
                                DEMO_SOURCE, CHECKSUM));
                alertService.evaluateNewObservation(period, observation, previous, now);
                previous = observation;
                recorded++;
            }
            period.recordPriceState(series.getValue().getLast(),
                    now.minus(Duration.ofDays(DEMO_DAYS.getLast())));
        }
        return new DemoResult(catalog.products().size(), recorded);
    }

    private static Map<String, List<BigDecimal>> demoSeries() {
        Map<String, List<BigDecimal>> series = new LinkedHashMap<>();
        series.put("DEMO-MILK|AEON", prices("10.00", "10.00", "9.50", "9.00", "8.80", "9.60", "9.60", "8.90"));
        series.put("DEMO-MILK|JASONS", prices("10.50", "10.50", "10.20", "9.80", "9.50", "10.00", "10.00", "9.70"));
        series.put("DEMO-BREAD|AEON", prices("11.50", "11.50", "11.50", "12.00", "12.00", "12.00", "12.00", "12.00"));
        series.put("DEMO-COFFEE|WELLCOME", prices("48.00", null, "47.50", "47.50", "45.00", "45.00", "46.00", "46.00"));
        series.put("DEMO-COFFEE|PARKNSHOP", prices("49.00", "49.00", "48.50", "48.50", "46.00", "46.00", "47.00", "47.00"));
        return series;
    }

    private static List<BigDecimal> prices(String... values) {
        List<BigDecimal> result = new ArrayList<>(values.length);
        for (String value : values) {
            result.add(value == null ? null : new BigDecimal(value));
        }
        return result;
    }

    private static ImportedCatalog demoCatalog() {
        List<ImportedCatalog.ImportedProduct> products = List.of(
                product("DEMO-MILK", "Demo Whole Milk 1L", "Demo Dairy", "AEON", "10.00", "JASONS", "10.50"),
                product("DEMO-BREAD", "Demo Sandwich Bread 400g", "Demo Bakery", "AEON", "11.50"),
                product("DEMO-COFFEE", "Demo Ground Coffee 200g", "Demo Coffee",
                        "WELLCOME", "48.00", "PARKNSHOP", "49.00"));
        int listings = products.stream().mapToInt(product -> product.listings().size()).sum();
        return new ImportedCatalog(products, listings, 0);
    }

    private static ImportedCatalog.ImportedProduct product(String code, String name, String category,
                                                            String... retailerAndPricePairs) {
        List<ImportedCatalog.ImportedListing> listings = new ArrayList<>();
        for (int index = 0; index < retailerAndPricePairs.length; index += 2) {
            listings.add(new ImportedCatalog.ImportedListing(retailerAndPricePairs[index],
                    new BigDecimal(retailerAndPricePairs[index + 1]), List.of()));
        }
        LocalizedText nameText = new LocalizedText(name, name, name);
        LocalizedText categoryText = new LocalizedText(category, category, category);
        return new ImportedCatalog.ImportedProduct(code, new LocalizedText("Demo brand", "示範品牌", "示范品牌"),
                nameText, new LocalizedText("Demo", "示範", "示范"), categoryText, categoryText, listings);
    }
}
