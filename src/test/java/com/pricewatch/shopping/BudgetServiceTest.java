package com.pricewatch.shopping;

import com.pricewatch.MutableClockConfig;
import com.pricewatch.alerts.persistence.BudgetSettingsEntity.Period;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.alerts.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MutableClockConfig.class)
class BudgetServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private BudgetService budget;
    @Autowired private BudgetSettingsRepository settings;
    @Autowired private AlertEventRepository events;
    @Autowired private AlertRuleRepository rules;
    @Autowired private PriceObservationRepository observations;
    @Autowired private TrackingChoiceRepository choices;
    @Autowired private WishlistEntryRepository wishlist;
    @Autowired private ProductRepository productRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private com.pricewatch.shopping.repository.ShoppingListItemRepository itemRepository;
    @Autowired private com.pricewatch.shopping.repository.ShoppingListRepository listRepository;

    @BeforeEach
    void clear() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        events.deleteAllInBatch();
        rules.deleteAllInBatch();
        settings.deleteAllInBatch();
        observations.deleteAllInBatch();
        choices.deleteAllInBatch();
        wishlist.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void quantityAndRetailerChangeTheEstimate() {
        catalog.importSample();
        budget.save(Period.WEEKLY, new BigDecimal("100"));
        shopping.add("P000000124", 1);
        BigDecimal autoKnown = budget.report().knownSubtotal();

        shopping.update("P000000124", 3, false);
        assertTrue(budget.report().knownSubtotal().compareTo(autoKnown) > 0, "quantity changes estimate");

        shopping.update("P000000124", 3, false, "JASONS");
        assertTrue(budget.report().knownSubtotal().compareTo(autoKnown) > 0);
        assertEquals(Period.WEEKLY, budget.report().period());
    }

    @Test
    void unknownPriceMarksEstimateIncomplete() {
        catalog.importSample();
        accept(product("KNOWN", "Milk", "[{\"supermarketCode\":\"AEON\",\"price\":\"2.00\"}]"),
                product("UNKNOWN", "Bread", "[{\"supermarketCode\":\"AEON\",\"price\":null}]"));
        shopping.add("KNOWN", 2);
        shopping.add("UNKNOWN", 1);
        BudgetReport report = budget.report();
        assertTrue(report.incomplete());
        assertEquals(1, report.unknownItemCount());
        assertEquals(new BigDecimal("4.0000"), report.knownSubtotal());
    }

    @Test
    void categoriesReconcileWithKnownSubtotal() {
        catalog.importSample();
        shopping.add("P000000124", 2);
        shopping.add("P000000229", 1);
        BudgetReport report = budget.report();
        BigDecimal sum = report.categories().stream().map(CategoryTotal::knownSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(report.knownSubtotal()),
                "category amounts must reconcile with the known subtotal");
        assertTrue(report.categories().stream().anyMatch(c -> c.category().equals("Milk / Milk drinks")));
    }

    @Test
    void overBudgetWarningUsesKnownSubtotalAndCheckedItemsStayIncluded() {
        catalog.importSample();
        budget.save(Period.MONTHLY, new BigDecimal("5"));
        shopping.add("P000000124", 2);
        shopping.update("P000000124", 2, true);
        BudgetReport report = budget.report();
        assertTrue(report.overBudget(), "known subtotal above planned limit");
        assertTrue(shopping.summary().knownSubtotal().compareTo(BigDecimal.ZERO) > 0,
                "checked items keep their J04 total behaviour");
    }

    private void accept(String... records) {
        catalog.accept(("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8), "SAMPLE", "test");
    }

    private String product(String code, String name, String prices) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"名稱","zh-Hans":"名称"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"%s","zh-Hant":"分類","zh-Hans":"分类"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮","zh-Hans":"鲜"},
                "prices":%s,"offers":[]}
                """.formatted(code, name, name, prices).trim();
    }
}
