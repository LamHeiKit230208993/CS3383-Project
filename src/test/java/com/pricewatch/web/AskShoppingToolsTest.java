package com.pricewatch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.budget.BudgetPeriod;
import com.pricewatch.budget.BudgetService;
import com.pricewatch.budget.BudgetState;
import com.pricewatch.budget.BudgetView;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.tracking.SavedProductView;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AskShoppingToolsTest {
    private static final Locale ENGLISH = Locale.ENGLISH;
    private static final String CODE = "MILK-2L";
    private static final long LIST = 25;
    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private final ObjectMapper json = new ObjectMapper();
    private final ShoppingService shopping = mock(ShoppingService.class);
    private final TrackingService tracking = mock(TrackingService.class);
    private final WatchRuleService watches = mock(WatchRuleService.class);
    private final CatalogService catalogue = mock(CatalogService.class);
    private final BudgetService budgets = mock(BudgetService.class);
    private AskShoppingTools tools;

    @BeforeEach
    void setup() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasenames("ask-shopping-messages", "alert-editor");
        messages.setDefaultEncoding(StandardCharsets.UTF_8.name());
        tools = new AskShoppingTools(catalogue, shopping, tracking, watches, mock(ProductTargetService.class),
                mock(ReminderService.class), mock(AlertService.class), budgets, messages, Clock.fixed(NOW, ZoneOffset.UTC));
        when(shopping.lists()).thenReturn(List.of(new ShoppingService.ListChoice(null, "My list"),
                new ShoppingService.ListChoice(LIST, "Weekend groceries")));
        when(catalogue.find(CODE)).thenReturn(product());
    }

    @Test
    void preparesOnceAndKeepsAnExistingRetailerWhenAddingToNamedCart() throws Exception {
        when(shopping.items(LIST)).thenReturn(List.of(item(3)));
        var turn = tools.open(ENGLISH, null, null);
        var args = json.readTree("""
                {"operation":"add","list_name":"Weekend groceries","product_code":"MILK-2L","quantity":2}
                """);
        assertEquals("prepared", json.valueToTree(turn.execute("propose_cart", args)).get("status").asText());
        turn.execute("propose_cart", args);
        var proposal = turn.operations().getFirst();
        assertEquals(1, turn.operations().size());
        assertFalse(proposal.requiresConfirmation());
        assertEquals("receipt", tools.apply(proposal, ENGLISH).kind());
        verify(shopping).add(LIST, CODE, 2, null);
    }

    @Test
    void removalWithAChangedQuantityRequiresARefreshedPreview() throws Exception {
        when(shopping.items(LIST)).thenReturn(List.of(item(3)));
        var turn = tools.open(ENGLISH, null, null);
        turn.execute("propose_cart", json.readTree("""
                {"operation":"remove","list_id":25,"product_code":"MILK-2L","quantity":1}
                """));
        var proposal = turn.operations().getFirst();
        assertTrue(proposal.requiresConfirmation());
        when(shopping.items(LIST)).thenReturn(List.of(item(4)));
        var changed = assertThrows(AskShoppingTools.StaleOperation.class, () -> tools.apply(proposal, ENGLISH));
        tools.apply(changed.operation(), ENGLISH);
        verify(shopping).editDetails(LIST, CODE, 3, "AEON");
    }

    @Test
    void retainsAlertDraftAcrossTurnsAndOnlySavesWhenApplied() throws Exception {
        when(tracking.savedProducts()).thenReturn(List.of(new SavedProductView(product(), NOW)));
        var first = tools.open(ENGLISH, null, null);
        var partial = first.execute("propose_alert", json.readTree("""
                {"operation":"create","product_code":"MILK-2L","condition":"combined","target":"25.50"}
                """));
        assertTrue(json.writeValueAsString(partial).contains("date and time in Hong Kong"));
        assertTrue(first.operations().isEmpty());

        var next = tools.open(ENGLISH, CODE, first.draft());
        var timestamp = LocalDateTime.parse("2026-11-23T17:30");
        next.execute("propose_alert", json.readTree("""
                {"operation":"create","start_at":"2026-11-23T17:30","repeat":"WEEKLY"}
                """));
        verifyNoInteractions(watches);
        var proposal = next.operations().getFirst();
        assertTrue(proposal.requiresConfirmation());
        assertTrue(proposal.preview().detail().contains("17:30 HKT"));
        tools.apply(proposal, ENGLISH);
        verify(watches).save(CODE, null, new BigDecimal("25.5000"), timestamp, "WEEKLY");
    }

    @Test
    void cartViewReportsAnIncompleteSubtotalAndSelectedBudget() throws Exception {
        when(shopping.items(LIST)).thenReturn(List.of(item(1), new ItemView(product(), 2, false, null, "OTHER", null, "OTHER")));
        when(budgets.forSummary(any())).thenReturn(new BudgetView(BudgetPeriod.WEEKLY, new BigDecimal("100"),
                new BigDecimal("28"), 1, List.of(), BudgetState.UNKNOWN_TOTAL));
        var turn = tools.open(Locale.forLanguageTag("zh-Hant"), null, null);
        var reply = json.valueToTree(turn.execute("get_cart", json.readTree("{\"list_id\":25}")));
        assertEquals(1, reply.get("unknownItemCount").asInt());
        var card = turn.cards().getFirst();
        assertTrue(card.detail().contains("總額未完整"));
        assertTrue(card.rows().getLast().detail().contains("價格不詳"));
        assertEquals(LIST, card.listId());
    }

    @Test
    void decimalCalculatorSupportsGroupedAmountsPercentagesAndInvalidInput() throws Exception {
        assertEquals("30", AskArithmetic.calculate("15% of (20+180)").toPlainString());
        assertEquals("-4.1", AskArithmetic.calculate("-(10 / 2) + .9").toPlainString());
        assertThrows(IllegalArgumentException.class, () -> AskArithmetic.calculate("1 / (4 - 4)"));
        assertThrows(IllegalArgumentException.class, () -> AskArithmetic.calculate("sqrt(4)"));
        var turn = tools.open(ENGLISH, null, null);
        turn.execute("calculate", json.readTree("{\"expression\":\"15% of 200\",\"currency\":true}"));
        assertTrue(turn.cards().getFirst().detail().contains("HK$30.00"));
    }

    private static ProductView product() {
        var text = new LocalizedText("Milk 2 L", "鮮奶 2 升", "鲜奶 2 升");
        var blank = new LocalizedText("", "", "");
        var price = new ListingView("AEON", new BigDecimal("28"), List.of());
        return new ProductView(CODE, blank, text, blank, blank, blank, true, List.of(price), price.price(), List.of(price));
    }

    private static ItemView item(int packs) {
        var price = new BigDecimal("28");
        return new ItemView(product(), packs, false, price, "AEON", price.multiply(BigDecimal.valueOf(packs)), "AEON");
    }
}
