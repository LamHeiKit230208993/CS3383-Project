package com.pricewatch.budget;

import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BudgetServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private BudgetService budget;
    @Autowired private BudgetSettingsRepository budgetRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;
    @Autowired private PriceObservationRepository observationRepository;
    @Autowired private TrackingChoiceRepository trackingRepository;
    @Autowired private WishlistEntryRepository wishlistRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void clearDatabase() {
        budgetRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        wishlistRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void categoryTotalsReconcileWithKnownSubtotalAndAreNotActualSpending() {
        catalog.importSample();
        shopping.add("P000000124", 2);
        shopping.add("P000000229", 3);

        BudgetView view = budget.view();
        BigDecimal categorySum = view.categories().stream()
                .map(BudgetView.CategoryTotal::knownSubtotal)
                .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);
        assertEquals(0, categorySum.compareTo(view.knownSubtotal()));
        assertEquals(view.knownSubtotal(), shopping.summary().knownSubtotal());
    }

    @Test
    void budgetWarningFollowsKnownSubtotalAndQuantityChanges() {
        catalog.importSample();
        shopping.add("P000000124", 2); // AEON 7.50 -> 15.00

        budget.setBudget(BudgetSettingsEntity.Period.WEEKLY, new BigDecimal("10.00"));
        BudgetView exceeded = budget.view();
        assertTrue(exceeded.exceeded());
        assertEquals(new BigDecimal("5.0000"), exceeded.overBy());

        shopping.update("P000000124", 1, false);
        BudgetView under = budget.view();
        assertFalse(under.exceeded());
        assertNull(under.overBy());
    }

    @Test
    void unknownPriceKeepsEstimateIncomplete() {
        catalog.importSample();
        shopping.add("P000000124", 1);
        budget.setBudget(BudgetSettingsEntity.Period.MONTHLY, new BigDecimal("100.00"));

        // Force the selected JASONS listing to disappear with a replacement.
        catalog.accept(("[{\"code\":\"OTHER\",\"brand\":{\"en\":\"B\",\"zh-Hant\":\"品\",\"zh-Hans\":\"品\"},"
                + "\"name\":{\"en\":\"x\",\"zh-Hant\":\"牛奶\",\"zh-Hans\":\"牛奶\"},"
                + "\"cat1Name\":{\"en\":\"Food\",\"zh-Hant\":\"食品\",\"zh-Hans\":\"食品\"},"
                + "\"cat2Name\":{\"en\":\"Milk\",\"zh-Hant\":\"牛奶\",\"zh-Hans\":\"牛奶\"},"
                + "\"cat3Name\":{\"en\":\"Fresh\",\"zh-Hant\":\"鮮奶\",\"zh-Hans\":\"鲜奶\"},"
                + "\"prices\":[{\"supermarketCode\":\"AEON\",\"price\":\"1.00\"}],\"offers\":[]}]")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8), "SAMPLE", "test");

        BudgetView view = budget.view();
        assertEquals(1, view.unknownItemCount());
        assertTrue(view.incomplete());
    }

    @Test
    void explicitRetailerKeepsChosenPriceEvenWhenCheaperExists() {
        catalog.importSample();
        // JASONS milk is 12.50; AEON lists 7.50. Explicit JASONS must stay 12.50.
        shopping.add("P000000124", 1, "JASONS");

        BudgetView view = budget.view();
        assertEquals(new BigDecimal("12.5000"), view.knownSubtotal());
    }

    @Test
    void clearBudgetRemovesSetting() {
        budget.setBudget(BudgetSettingsEntity.Period.WEEKLY, new BigDecimal("50.00"));
        assertTrue(budget.view().configured());
        budget.clearBudget();
        assertFalse(budget.view().configured());
    }

    @Test
    void rejectsNonPositiveBudget() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> budget.setBudget(BudgetSettingsEntity.Period.WEEKLY, BigDecimal.ZERO));
    }
}
