package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:web;DB_CLOSE_DELAY=-1", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class HelloControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired ShoppingService shopping;
    @MockitoBean DeepSeekChatClient provider;

    private final ObjectMapper json = new ObjectMapper();

    @Test void offlineChecklistDownloadsCheckedItemsAndTheirSavedPrices() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        shopping.update("P000000124", 2, true);
        var session = new MockHttpSession();
        mvc.perform(get("/list/offline").session(session)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"shopping-checklist.html\""))
                .andExpect(content().string(containsString("Prices retrieved")))
                .andExpect(content().string(containsString("Historical sample")))
                .andExpect(content().string(containsString(catalog.status().retrievedAt().toString())))
                .andExpect(content().string(containsString("2 · packs")))
                .andExpect(content().string(containsString("JASONS")))
                .andExpect(content().string(containsString("checked=\"checked\"")))
                .andExpect(content().string(containsString("HK$12.50")))
                .andExpect(content().string(containsString("HK$25.00")))
                .andExpect(content().string(not(containsString("<script"))))
                .andExpect(content().string(not(containsString("<link"))))
                .andExpect(content().string(not(containsString("<form"))));
        var listId = shopping.lists().getFirst().id();
        mvc.perform(post("/offline/enter").session(session).param("listId", listId.toString()))
                .andExpect(redirectedUrl("/list?listId=" + listId));
        mvc.perform(post("/offline/exit").session(session).header("Origin", "http://example.com"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/ask").session(session)).andExpect(status().isOk());
        assertEquals(2, shopping.items().getFirst().quantity());
    }

    @Test void offlineChecklistEscapesImportedLabelsAndKeepsUnknownPrices() throws Exception {
        long listId = shopping.importList(new ByteArrayInputStream("""
                {"version":1,"name":"<b>Trip</b>","items":[{"productCode":"MISSING",
                "displayName":"<img src=x>","quantity":1,"checked":false,"retailerCode":"TEST"}]}
                """.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/offline")).andExpect(redirectedUrl("/list"));
        mvc.perform(get("/list/offline").param("listId", "-1")).andExpect(status().isNotFound());
        mvc.perform(get("/list/offline").param("listId", Long.toString(listId))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Price retrieval time is unavailable.")))
                .andExpect(content().string(containsString("&lt;b&gt;Trip&lt;/b&gt;")))
                .andExpect(content().string(containsString("&lt;img src=x&gt;")))
                .andExpect(content().string(not(containsString("<img"))))
                .andExpect(content().string(containsString("TEST")))
                .andExpect(content().string(containsString("Unknown")));
        mvc.perform(post("/preferences").param("language", "zh-Hant").param("highContrast", "true"))
                .andExpect(redirectedUrl("/preferences"));
        mvc.perform(get("/list/offline").param("listId", Long.toString(listId))).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hant\"")))
                .andExpect(content().string(containsString("class=\"high-contrast\"")))
                .andExpect(content().string(containsString("價格擷取時間不詳。")));
    }

    @Test void emptyStartDoesNotPretendToHavePrices() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk()).andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("No prices loaded")))
                .andExpect(content().string(containsString("Find a product")))
                .andExpect(content().string(not(containsString("DEMO-MILK"))));
        assertNull(catalog.status());
    }
    @Test void askKeepsTheExactPackAcrossModelToolFollowUps() throws Exception {
        when(provider.reply(anyString(), anyList(), anyList(), any())).thenAnswer(this::catalogueReply);
        var session = new MockHttpSession();
        mvc.perform(get("/ask").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ask about prices")));
        catalog.importSample();
        String firstChat = mvc.perform(post("/ask").session(session)
                        .param("question", "Where is Kowloon Dairy’s 236 mL fresh milk cheapest?"))
                .andExpect(redirectedUrlPattern("/ask?chatId=*")).andReturn().getResponse().getRedirectedUrl();
        String firstId = firstChat.substring(firstChat.indexOf('=') + 1);
        mvc.perform(get(firstChat).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Here are the saved prices for this pack.")))
                .andExpect(content().string(containsString("HK$7.50")))
                .andExpect(content().string(containsString("Lowest listed")));
        assertEquals(List.of("AEON", "JASONS", "PARKNSHOP", "WELLCOME"), lastPriceRetailers(session));
        mvc.perform(post("/ask").session(session).param("chatId", firstId).param("question", "What about Wellcome?"))
                .andExpect(redirectedUrl(firstChat));
        mvc.perform(get(firstChat).session(session))
                .andExpect(content().string(containsString("What about Wellcome?")))
                .andExpect(content().string(containsString("HK$12.00")));
        assertEquals(List.of("WELLCOME"), lastPriceRetailers(session));
        mvc.perform(post("/ask").session(session).param("chatId", firstId).param("question", "no-such-product"))
                .andExpect(redirectedUrl(firstChat));
        mvc.perform(get(firstChat).session(session))
                .andExpect(content().string(containsString("What name is printed on the pack?")));
        assertTrue(lastPriceRetailers(session).isEmpty());
        mvc.perform(post("/ask").session(session).param("chatId", firstId).param("question", "And at AEON?"))
                .andExpect(redirectedUrl(firstChat));
        mvc.perform(get(firstChat).session(session))
                .andExpect(content().string(containsString("HK$7.50")));
        assertEquals(List.of("AEON"), lastPriceRetailers(session));
        mvc.perform(post("/ask").session(session).param("chatId", firstId).param("question", "Which is cheapest?"))
                .andExpect(redirectedUrl(firstChat));
        mvc.perform(get(firstChat).session(session))
                .andExpect(content().string(containsString("Which is cheapest?")))
                .andExpect(content().string(containsString("Lowest listed")));
        mvc.perform(get("/ask").session(session))
                .andExpect(content().string(not(containsString("What about Wellcome?"))));
        String secondChat = mvc.perform(post("/ask").session(session).param("question", "no-such-product"))
                .andExpect(redirectedUrlPattern("/ask?chatId=*")).andReturn().getResponse().getRedirectedUrl();
        assertNotEquals(firstChat, secondChat);
        mvc.perform(get(secondChat).session(session))
                .andExpect(content().string(containsString("What name is printed on the pack?")))
                .andExpect(content().string(not(containsString("HK$7.50"))));
        mvc.perform(get(firstChat).session(session))
                .andExpect(content().string(containsString("What about Wellcome?")));
        mvc.perform(get(firstChat).session(new MockHttpSession()))
                .andExpect(content().string(not(containsString("What about Wellcome?"))));
    }

    @Test void aStaleChatIdStartsFreshWithoutLosingTheSubmittedQuestion() throws Exception {
        when(provider.reply(anyString(), anyList(), anyList(), any())).thenAnswer(this::catalogueReply);
        catalog.importSample();
        var session = new MockHttpSession();
        String oldChatId = "17efcb62-d81d-4bd1-b62b-c8e2cf207259";
        String chatUrl = mvc.perform(post("/ask").session(session)
                        .param("chatId", oldChatId).param("question", "P000000124"))
                .andExpect(redirectedUrlPattern("/ask?chatId=*")).andReturn().getResponse().getRedirectedUrl();
        assertNotEquals("/ask?chatId=" + oldChatId, chatUrl);
        mvc.perform(get(chatUrl).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(containsString("HK$7.50")));
    }

    @Test void unavailableProviderKeepsTheSubmittedQuestionForNativeRetry() throws Exception {
        var session = new MockHttpSession();
        String question = "P000000124";
        when(provider.reply(anyString(), anyList(), anyList(), any()))
                .thenReturn(Optional.empty())
                .thenAnswer(this::catalogueReply);
        catalog.importSample();
        String failedChat = mvc.perform(post("/ask").session(session).param("question", question))
                .andExpect(redirectedUrlPattern("/ask?chatId=*&replyFailed=true")).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(get(failedChat).session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("awaitingReply", true))
                .andExpect(model().attribute("replyFailed", true))
                .andExpect(content().string(containsString(question)))
                .andExpect(content().string(not(containsString("HK$7.50"))));
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString(question)));
        mvc.perform(get("/list").session(session)).andExpect(status().isOk());
        String chatUrl = failedChat.substring(0, failedChat.indexOf('&'));
        mvc.perform(post("/ask/retry").session(session).param("chatId", chatUrl.substring(chatUrl.indexOf('=') + 1)))
                .andExpect(redirectedUrl(chatUrl));
        mvc.perform(get(chatUrl).session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("awaitingReply", false))
                .andExpect(content().string(containsString("HK$7.50")));
    }

    private Optional<String> catalogueReply(InvocationOnMock invocation) {
        List<DeepSeekChatClient.ChatMessage> history = invocation.getArgument(1);
        BiFunction<String, JsonNode, Object> tool = invocation.getArgument(3);
        String question = history.getLast().content();
        if (question.equals("no-such-product")) {
            assertTrue(json.valueToTree(tool.apply("search_products", json.createObjectNode().put("query", question)))
                    .path("products").isEmpty());
            return Optional.of("I couldn't find that product. What name is printed on the pack?");
        }
        var priceArguments = json.createObjectNode();
        switch (question) {
            case "What about Wellcome?" -> priceArguments.put("retailer", "惠康 WELLCOME");
            case "And at AEON?" -> priceArguments.put("retailer", "AEON");
            case "Which is cheapest?" -> { }
            default -> {
                tool.apply("search_products", json.createObjectNode().put("query", "P000000124"));
                priceArguments.put("product_code", "P000000124");
            }
        }
        JsonNode checked = json.valueToTree(tool.apply("get_product_prices", priceArguments));
        assertEquals("P000000124", checked.path("product_code").asText());
        assertEquals("SAMPLE", checked.path("source").path("origin").asText());
        return Optional.of("Here are the saved prices for this pack.");
    }

    private static List<String> lastPriceRetailers(MockHttpSession session) {
        return AskController.history(session).getFirst().messages().getLast().priceCards().stream()
                .flatMap(card -> card.rows().stream()).map(AskCatalogueTools.PriceRow::retailer).toList();
    }
    @Test void sampleImportAndThreeLanguageSearchRenderSourceIdentity() throws Exception {
        mvc.perform(post("/data/sample")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/data"));
        for (String query : new String[]{"100 %Fresh Milk", "鮮牛奶", "鲜牛奶"}) {
            mvc.perform(get("/").param("q", query)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("P000000124")))
                    .andExpect(content().string(containsString("Historical sample")));
        }
        mvc.perform(get("/products/P000000229")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Buy 3 at $30.00")))
                .andExpect(content().string(containsString("Lowest listed")));
    }
    @Test void formsAddEditCheckAndRemoveWithoutJavaScript() throws Exception {
        catalog.importSample();
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "2"))
                .andExpect(redirectedUrl("/list"));
        mvc.perform(post("/list/items/P000000124/update").param("quantity", "3").param("checked", "true"))
                .andExpect(redirectedUrl("/list"));
        assertEquals(3, shopping.items().getFirst().quantity());
        assertTrue(shopping.items().getFirst().checked());
        mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(content().string(containsString("22.50")))
                .andExpect(content().string(containsString("Estimated list cost")));
        mvc.perform(post("/list/items/P000000124/remove")).andExpect(redirectedUrl("/list"));
        assertTrue(shopping.items().isEmpty());
    }
    @Test void validationAndUnknownRoutesDoNotWriteInvalidData() throws Exception {
        catalog.importSample();
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "0"))
                .andExpect(redirectedUrl("/list")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1.5"))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("Check your input")));
        assertTrue(shopping.items().isEmpty());
        mvc.perform(get("/products/UNKNOWN")).andExpect(status().isNotFound());
        mvc.perform(get("/planned/UNKNOWN")).andExpect(status().isNotFound());
    }
    @Test void replacingAnAcceptedCatalogueRequiresServerSideConfirmation() throws Exception {
        var previous = catalog.importSample();
        mvc.perform(post("/data/sample")).andExpect(redirectedUrl("/data"))
                .andExpect(flash().attributeExists("error"));
        assertEquals(previous.retrievedAt(), catalog.status().retrievedAt());
        mvc.perform(post("/data/sample").param("confirmSample", "true"))
                .andExpect(redirectedUrl("/data")).andExpect(flash().attributeExists("notice"));
    }
    @Test void unrelatedSitesCannotSubmitLocalChanges() throws Exception {
        mvc.perform(post("/data/sample").header("Origin", "https://unrelated.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/data/sample").header("Origin", "null")).andExpect(status().isForbidden());
        mvc.perform(post("/data/sample").header("Sec-Fetch-Site", "cross-site")).andExpect(status().isForbidden());
        mvc.perform(get("/").with(request -> { request.setServerName("unrelated.example"); return request; }))
                .andExpect(status().isForbidden());
        assertNull(catalog.status());
        mvc.perform(post("/data/sample").header("Origin", "http://localhost"))
                .andExpect(redirectedUrl("/data"));
        assertNotNull(catalog.status());
    }
    @Test void chosenRetailerIsSavedAndCanBeChangedExplicitly() throws Exception {
        catalog.importSample();
        mvc.perform(get("/products/P000000124")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose where to buy.")))
                .andExpect(content().string(containsString("name=\"retailerCode\"")))
                .andExpect(content().string(containsString("Offer not included in estimate")));
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "2")
                        .param("retailerCode", "JASONS")).andExpect(redirectedUrl("/list"));
        assertEquals("JASONS", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(0, shopping.items().getFirst().lineTotal().compareTo(new java.math.BigDecimal("25.00")));
        mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"JASONS\"\\s+selected=\"selected\">.*")));
        mvc.perform(post("/list/items/P000000124/update").param("quantity", "2")
                        .param("retailerCode", "PARKNSHOP").param("checked", "true"))
                .andExpect(redirectedUrl("/list"));
        assertEquals("PARKNSHOP", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(0, shopping.items().getFirst().lineTotal().compareTo(new java.math.BigDecimal("18.00")));
        mvc.perform(post("/list/items/P000000124/update").param("quantity", "2")
                        .param("retailerCode", "UNKNOWN")).andExpect(flash().attributeExists("error"));
        assertEquals("PARKNSHOP", shopping.items().getFirst().selectedRetailerCode());
    }
    @Test void dataIsANormalDestinationAndCategoryFiltersUseSourceLabels() throws Exception {
        catalog.importSample();
        mvc.perform(get("/data")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Price data &amp; updates")))
                .andExpect(content().string(not(containsString("COMING LATER"))));
        mvc.perform(get("/").param("category", "Milk / Milk drinks"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(not(containsString("P000000229"))))
                .andExpect(content().string(containsString("category-filter")));
        mvc.perform(get("/").param("q", "food").param("related", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("related", true));
    }
    @Test void newSearchDoesNotCarryRelatedModeFromThePreviousQuery() throws Exception {
        catalog.importSample();
        mvc.perform(get("/").param("q", "Bakery").param("related", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("related", true))
                .andExpect(content().string(containsString("P000000229")))
                .andExpect(content().string(not(containsString("name=\"related\""))));
        mvc.perform(get("/").param("q", "milk"))
                .andExpect(model().attribute("related", false))
                .andExpect(content().string(containsString("P000000124")));
    }

    @Test void blankQueryShowsCatalogueEvenWithAStaleRelatedParameter() throws Exception {
        catalog.importSample();
        for (String query : new String[]{"", "   "}) {
            mvc.perform(get("/").param("q", query).param("related", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("related", false))
                    .andExpect(model().attribute("resultCount", 2))
                    .andExpect(content().string(containsString("P000000124")))
                    .andExpect(content().string(containsString("P000000229")));
        }
    }

    @Test void laterFeaturesAreClearlyUnavailable() throws Exception {
        for (String feature : new String[]{"ask"}) {
            mvc.perform(get("/planned/" + feature)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("PLANNED · NOT AVAILABLE YET")))
                    .andExpect(content().string(not(containsString("chat-form"))));
        }
        mvc.perform(get("/data")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No automatic refresh runs at startup")));
    }
    @Test void oldPlannedLinksLeadToWorkingMilestonePages() throws Exception {
        mvc.perform(get("/planned/saved")).andExpect(redirectedUrl("/saved"));
        mvc.perform(get("/planned/updates")).andExpect(redirectedUrl("/alerts"));
    }
    @Test void sourceTextIsEscapedAndSearchPaginationIsBounded() throws Exception {
        catalog.importSample();
        mvc.perform(get("/").param("q", "<script>alert(1)</script>").param("page", "2147483647"))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("value=\"<script>"))))
                .andExpect(content().string(containsString("No matching products")));
        mvc.perform(get("/").param("page", "-1")).andExpect(status().isOk());
    }
}
