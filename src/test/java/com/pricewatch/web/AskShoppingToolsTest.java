package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.pricewatch.notifications.AlertEventView;
import com.pricewatch.notifications.ProductTargetEventView;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.notifications.WatchRuleView;
import com.pricewatch.notifications.persistence.ProductTargetRetailerPrice;
import com.pricewatch.reminder.ReminderOccurrenceView;
import com.pricewatch.reminder.ReminderOverview;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.reminder.ReminderView;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.tracking.SavedProductView;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.TrackingView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private final ProductTargetService targets = mock(ProductTargetService.class);
    private final ReminderService reminders = mock(ReminderService.class);
    private final AlertService alerts = mock(AlertService.class);
    private final CatalogService catalogue = mock(CatalogService.class);
    private final BudgetService budgets = mock(BudgetService.class);
    private AskShoppingTools tools;

    @BeforeEach
    void setup() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasenames("ask-shopping-messages", "alert-editor");
        messages.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messages.setFallbackToSystemLocale(false);
        tools = new AskShoppingTools(catalogue, shopping, tracking, watches, targets,
                reminders, alerts, budgets, messages, Clock.fixed(NOW, ZoneOffset.UTC));
        when(shopping.lists()).thenReturn(List.of(new ShoppingService.ListChoice(null, "My list"),
                new ShoppingService.ListChoice(LIST, "Weekend groceries")));
        when(catalogue.find(CODE)).thenReturn(product());
        when(reminders.overview()).thenReturn(new ReminderOverview(List.of()));
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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"+5 − 3 × 2| -1", "10 ÷ 4|2.5", "100% OF 20|20"})
    void calculatorPreservesOperatorPrecedenceAndNormalizesCommonSymbols(String expression, String result) {
        assertEquals(result, AskArithmetic.calculate(expression).toPlainString());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "(2+3", "2 3", "1..0"})
    void calculatorRejectsBlankIncompleteAndNonDecimalSyntax(String expression) {
        assertThrows(IllegalArgumentException.class, () -> AskArithmetic.calculate(expression));
    }

    @Test
    void calculatorCapsInputLengthEvenForValidNumbers() {
        assertEquals("1".repeat(120), AskArithmetic.calculate("1".repeat(120)).toPlainString());
        assertThrows(IllegalArgumentException.class, () -> AskArithmetic.calculate("1".repeat(121)));
    }

    @Test
    void listCreationAndRenamePrepareWithoutWritingAndDeletionRequiresConfirmation() throws Exception {
        when(shopping.createList("Party")).thenReturn(36L);
        when(shopping.items(LIST)).thenReturn(List.of(item(2)));
        var create = tools.open(ENGLISH, null, null);
        assertEquals("prepared", execute(create, "propose_cart", """
                {"operation":"create_list","new_name":"Party"}
                """).path("status").asText());
        verify(shopping, never()).createList("Party");
        assertEquals(36L, tools.apply(create.operations().getFirst(), ENGLISH).listId());

        var rename = tools.open(ENGLISH, null, null);
        assertEquals("prepared", execute(rename, "propose_cart", """
                {"operation":"rename_list","list_id":25,"list_name":"weekend groceries","new_name":"Saturday"}
                """).path("status").asText());
        assertTrue(tools.apply(rename.operations().getFirst(), ENGLISH).detail().contains("Saturday"));
        verify(shopping).renameList(LIST, "Saturday");

        var delete = tools.open(ENGLISH, null, null);
        assertEquals("awaiting_confirmation", execute(delete, "propose_cart", """
                {"operation":"delete_list","list_id":25}
                """).path("status").asText());
        var proposal = delete.operations().getFirst();
        assertTrue(proposal.preview().detail().contains("Items: 1"), proposal.preview().detail());
        assertNull(tools.apply(proposal, ENGLISH).listId());
        verify(shopping).deleteList(LIST);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"operation\":\"create_list\",\"new_name\":\"my list\"}|not reserved",
            "{\"operation\":\"create_list\",\"new_name\":\"weekend groceries\"}|already has that name",
            "{\"operation\":\"delete_list\"}|cannot be renamed or deleted",
            "{\"operation\":\"add\",\"list_id\":25,\"list_name\":\"Another\"}|do not refer to the same list",
            "{\"operation\":\"add\",\"list_id\":0}|valid list ID",
            "{\"operation\":\"add\",\"list_id\":26}|could not be found",
            "{\"operation\":\"add\",\"list_name\":\"Another\"}|choose the exact saved list",
            "{\"operation\":\"add\"}|Choose an exact product",
            "{\"operation\":\"add\",\"product_code\":\"MILK-2L\",\"quantity\":0}|between 1 and 999",
            "{\"operation\":\"add\",\"product_code\":\"MILK-2L\",\"quantity\":1000}|between 1 and 999",
            "{\"operation\":\"add\",\"product_code\":\"MILK-2L\",\"quantity\":1.5}|whole number",
            "{\"operation\":\"remove\",\"product_code\":\"MILK-2L\"}|no longer in the list",
            "{\"operation\":\"add\",\"product_code\":\"MILK-2L\",\"retailer\":\"UNKNOWN\"}|current saved price",
            "{\"operation\":\"checkout\"}|supported cart operation"
    })
    void rejectsInvalidCartToolArgumentsBeforePreparingAChange(String arguments, String detail) throws Exception {
        var turn = tools.open(ENGLISH, null, null);
        assertTrue(execute(turn, "propose_cart", arguments).path("error").asText().contains(detail));
        assertTrue(turn.operations().isEmpty());
    }

    @Test
    void wishlistRemovalUsesSavedPackAndRequiresAnExplicitProposal() throws Exception {
        when(tracking.savedProducts()).thenReturn(List.of(new SavedProductView(product(), NOW)));
        var turn = tools.open(ENGLISH, CODE, null);
        var result = execute(turn, "get_wishlist", "{}");
        assertEquals(CODE, result.path("items").get(0).path("productCode").asText());
        assertEquals("awaiting_confirmation", execute(turn, "propose_wishlist", """
                {"operation":"remove"}
                """).path("status").asText());
        verify(tracking, never()).removeSaved(CODE);
        tools.apply(turn.operations().getFirst(), ENGLISH);
        verify(tracking).removeSaved(CODE);
    }

    @Test
    void calendarDraftCanBeInspectedAndDiscardedWithoutWritingARule() throws Exception {
        var turn = tools.open(ENGLISH, CODE, null);
        assertEquals("no_draft", execute(turn, "propose_alert", """
                {"operation":"draft"}
                """).path("status").asText());
        execute(turn, "propose_alert", """
                {"operation":"create","condition":"calendar","start_at":"2026-11-23T17:30"}
                """);
        var draft = execute(turn, "propose_alert", """
                {"operation":"draft"}
                """);
        assertEquals("calendar", draft.path("draft").path("condition").asText());
        assertEquals("repeat schedule or one-time", draft.path("missing").get(0).asText());
        assertEquals("draft_discarded", execute(turn, "propose_alert", """
                {"operation":"discard_draft"}
                """).path("status").asText());
        assertTrue(turn.operations().isEmpty());
        verifyNoInteractions(watches);
    }

    @Test
    void existingAlertTypesCanBeListedAndDispatchedToTheirOwningServices() throws Exception {
        var price = new BigDecimal("28");
        when(watches.allRules()).thenReturn(List.of(new WatchRuleView(7L, CODE, price, null, NOW,
                true, true, true, "WEEKLY", LocalDateTime.parse("2026-11-23T17:30"), false)));
        when(targets.rules()).thenReturn(List.of(new ProductTargetService.RuleView(CODE, price, true)));
        when(tracking.legacyRules()).thenReturn(List.of(new TrackingView(8L, CODE, "Milk 2 L", "AEON",
                true, NOW, null, price, true, true, List.of())));
        when(reminders.overview()).thenReturn(new ReminderOverview(List.of(
                new ReminderView(9L, CODE, "Milk 2 L", "", 1, 7, "AEON", true, true, NOW, NOW, false, true))));

        var turn = tools.open(ENGLISH, CODE, null);
        var rules = execute(turn, "get_alerts", """
                {"view":"rules","product_code":"MILK-2L"}
                """).path("rules");
        assertEquals(List.of("watch:7", "target:MILK-2L", "retailer:8", "reminder:9"),
                java.util.stream.StreamSupport.stream(rules.spliterator(), false)
                        .map(rule -> rule.path("rule_id").asText()).toList());
        var calendarDetail = rules.get(0).path("detail").asText();
        assertTrue(calendarDetail.contains("Waiting for a qualifying live price"), calendarDetail);

        confirmAlert("""
                {"operation":"toggle","rule_id":"watch:7","enabled":false}
                """);
        verify(watches).setEnabled(CODE, 7L, false);
        confirmAlert("""
                {"operation":"delete","rule_id":"target:MILK-2L"}
                """);
        verify(targets).deleteTarget(CODE);
        confirmAlert("""
                {"operation":"update","rule_id":"retailer:8","target":23}
                """);
        verify(tracking).configureTarget(8L, new BigDecimal("23.0000"));
        confirmAlert("""
                {"operation":"update","rule_id":"reminder:9","interval_days":14,"retailer":"automatic"}
                """);
        verify(reminders).update(9L, 1, 14, null);
    }

    @Test
    void alertHistoryCombinesProvidersAndSortsByOccurrenceTime() throws Exception {
        when(alerts.events(NOW)).thenReturn(List.of(
                new AlertEventView(1L, "target", "", CODE, "Milk", "AEON", BigDecimal.TEN,
                        NOW, null, null, NOW.minusSeconds(120), NOW, null, false, true, null, null),
                new AlertEventView(2L, "summary", "", null, null, null, null,
                        null, null, null, NOW, NOW, null, false, true, 1, null)));
        when(targets.events(NOW)).thenReturn(List.of(
                new ProductTargetEventView(3L, CODE, BigDecimal.TEN, NOW, NOW.minusSeconds(60),
                        List.of(new ProductTargetRetailerPrice("AEON", BigDecimal.TEN)),
                        false, true, null, null, 7L, null)));
        when(reminders.occurrences(NOW)).thenReturn(List.of(
                new ReminderOccurrenceView(4L, 9L, CODE, 2, "AEON", NOW, NOW,
                        false, true, true, true)));
        var turn = tools.open(ENGLISH, CODE, null);
        var events = execute(turn, "get_alerts", """
                {"view":"history","product_code":"MILK-2L"}
                """).path("events");
        assertEquals(3, events.size());
        var reminderDetail = events.get(0).path("detail").asText();
        assertTrue(reminderDetail.contains("Packs: 2"), reminderDetail);
        assertTrue(events.get(1).path("detail").asText().contains("Price retrieved"));
        assertTrue(events.get(2).path("detail").asText().contains("HK$10.00 · AEON"));
    }

    @Test
    void calendarOnlyAlertNeedsNoSavedWishlistItemAndRejectsReenablingACompletedSchedule() throws Exception {
        confirmAlert("""
                {"operation":"create","product_code":"MILK-2L","condition":"calendar",
                 "start_at":"2026-11-24T18:00","repeat":"ONCE"}
                """);
        verify(watches).save(CODE, null, null, LocalDateTime.parse("2026-11-24T18:00"), "ONCE");
        when(watches.allRules()).thenReturn(List.of(new WatchRuleView(7, CODE,
                null, null, null, false, false, false, "ONCE", LocalDateTime.parse("2026-11-19T16:00"), true)));
        var turn = tools.open(ENGLISH, CODE, null);
        assertTrue(execute(turn, "get_alerts", "{}").path("rules").get(0).path("detail").asText()
                .contains("Completed"));
        assertRejected(turn, "propose_alert", """
                {"operation":"toggle","rule_id":"watch:7","enabled":true}
                """, "one-time rule has completed");
    }

    @Test
    void aPreviewForAnExistingAlertCannotOverwriteNewerRuleSettings() throws Exception {
        when(targets.rules()).thenReturn(List.of(new ProductTargetService.RuleView(CODE, BigDecimal.TEN, true)));
        var turn = tools.open(ENGLISH, CODE, null);
        assertEquals("awaiting_confirmation", execute(turn, "propose_alert", """
                {"operation":"update","rule_id":"target:MILK-2L","target":"12.25"}
                """).path("status").asText());
        when(targets.rules()).thenReturn(List.of(new ProductTargetService.RuleView(CODE, new BigDecimal("11"), false)));
        var change = assertThrows(AskShoppingTools.StaleOperation.class,
                () -> tools.apply(turn.operations().getFirst(), ENGLISH));
        verify(targets, never()).setTarget(any(), any());
        assertEquals("receipt", tools.apply(change.operation(), ENGLISH).kind());
        verify(targets).setTarget(CODE, new BigDecimal("12.2500"));
    }

    @Test
    void legacyAndCalendarWatchRulesKeepTheirOwnUpdatePaths() throws Exception {
        var scheduled = LocalDateTime.parse("2026-11-24T18:00");
        when(watches.allRules()).thenReturn(List.of(
                new WatchRuleView(7, CODE, BigDecimal.TEN, 7, null, true, false, true, null, null, false),
                new WatchRuleView(8, CODE, BigDecimal.TEN, null, NOW, true, false, true, "ONCE", scheduled, false)));
        confirmAlert("""
                {"operation":"update","rule_id":"watch:7","target":"15.25","interval_days":14}
                """);
        verify(watches).save(CODE, 7L, new BigDecimal("15.2500"), 14);
        confirmAlert("""
                {"operation":"update","rule_id":"watch:8","repeat":"WEEKLY"}
                """);
        verify(watches).save(CODE, 8L, BigDecimal.TEN, scheduled, "WEEKLY");
        confirmAlert("""
                {"operation":"delete","rule_id":"watch:8"}
                """);
        verify(watches).delete(CODE, 8L);

        var turn = tools.open(ENGLISH, CODE, null);
        assertRejected(turn, "propose_alert", """
                {"operation":"update","rule_id":"watch:7","interval_days":366}
                """, "interval from 1 to 365 days");
        assertRejected(turn, "propose_alert", """
                {"operation":"update","rule_id":"watch:7","repeat":"WEEKLY"}
                """, "new calendar rule");
        assertRejected(turn, "propose_alert", """
                {"operation":"update","rule_id":"watch:8","interval_days":-1}
                """, "valid number of days");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"operation\":\"create\",\"condition\":\"custom\"}|Choose price, calendar, or combined",
            "{\"operation\":\"create\",\"condition\":\"price\",\"target\":\"-1\"}|target amount in HKD",
            "{\"operation\":\"create\",\"condition\":\"price\",\"target\":\"12.34567\"}|four decimal places",
            "{\"operation\":\"create\",\"condition\":\"price\",\"target\":\"9999999999999999\"}|target amount in HKD",
            "{\"operation\":\"create\",\"condition\":\"calendar\",\"start_at\":\"2026-02-30T23:00\"}|YYYY-MM-DDTHH:mm",
            "{\"operation\":\"create\",\"condition\":\"calendar\",\"start_at\":\"0000-01-01T00:00\"}|valid Hong Kong calendar",
            "{\"operation\":\"create\",\"condition\":\"calendar\",\"start_at\":\"+10000-01-01T00:00\"}|valid Hong Kong calendar",
            "{\"operation\":\"create\",\"condition\":\"calendar\",\"repeat\":\"FOREVER\"}|one-time, daily",
            "{\"operation\":\"create\",\"condition\":\"price\",\"target\":\"20\",\"repeat\":\"ONCE\"}|Save this pack to Wishlist",
            "{\"operation\":\"delete\",\"rule_id\":\"watch:99\"}|rule could not be found",
            "{\"operation\":\"delete\"}|Provide rule_id"
    })
    void invalidAlertArgumentsCannotProduceAConfirmation(String arguments, String detail) throws Exception {
        assertRejected(tools.open(ENGLISH, CODE, null), "propose_alert", arguments, detail);
    }

    @Test
    void cartToolListsTheDefaultCartAndCapsPreparedChanges() throws Exception {
        var turn = tools.open(ENGLISH, CODE, null);
        var lists = execute(turn, "list_carts", "{}").path("lists");
        assertEquals("Cart", lists.get(0).path("name").asText());
        assertEquals(LIST, lists.get(1).path("id").asLong());
        for (int i = 1; i <= 5; i++) {
            assertEquals("prepared", execute(turn, "propose_cart", """
                    {"operation":"create_list","new_name":"List %d"}
                    """.formatted(i)).path("status").asText());
        }
        String error = execute(turn, "propose_cart", """
                {"operation":"create_list","new_name":"List 6"}
                """).path("error").asText();
        assertTrue(error.contains("Up to five changes"), error);
        assertEquals(5, turn.operations().size());
        verify(shopping, never()).createList(any());
    }

    @Test
    void conflictingPacksAndRetailerEditsLeaveTheOriginalPreparedChangeIntact() throws Exception {
        when(shopping.items(LIST)).thenReturn(List.of(item(3)));
        var turn = tools.open(ENGLISH, CODE, null);
        assertRejected(turn, "propose_cart", """
                {"operation":"remove","list_id":25,"quantity":4}
                """, "fewer packs");
        assertRejected(turn, "propose_cart", """
                {"operation":"add","list_id":25,"quantity":2,"retailer":"automatic"}
                """, "different retailer choice");
        assertEquals("prepared", execute(turn, "propose_cart", """
                {"operation":"add","list_id":25,"quantity":2,"retailer":"AEON"}
                """).path("status").asText());
        String error = execute(turn, "propose_cart", """
                {"operation":"add","list_id":25,"quantity":5,"retailer":"AEON"}
                """).path("error").asText();
        assertTrue(error.contains("Only one change"), error);
        assertEquals(1, turn.operations().size());
        tools.apply(turn.operations().getFirst(), ENGLISH);
        verify(shopping).add(LIST, CODE, 2, "AEON");
    }

    private void assertRejected(AskShoppingTools.Turn turn, String tool, String arguments, String detail)
            throws Exception {
        String error = execute(turn, tool, arguments).path("error").asText();
        assertTrue(error.contains(detail), error);
        assertTrue(turn.operations().isEmpty());
    }

    private void confirmAlert(String arguments) throws Exception {
        var turn = tools.open(ENGLISH, CODE, null);
        assertEquals("awaiting_confirmation", execute(turn, "propose_alert", arguments).path("status").asText());
        assertEquals("receipt", tools.apply(turn.operations().getFirst(), ENGLISH).kind());
    }

    private JsonNode execute(AskShoppingTools.Turn turn, String tool, String arguments) throws Exception {
        return json.valueToTree(turn.execute(tool, json.readTree(arguments)));
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
