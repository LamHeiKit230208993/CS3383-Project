package com.pricewatch.chat;

import com.pricewatch.catalog.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:chat;DB_CLOSE_DELAY=-1", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class ChatControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired ChatService chat;
    @MockitoBean AiClient ai;

    @BeforeEach
    void sampleLoaded() {
        catalog.importSample();
    }

    @Test void agentSearchesThenPricesThenAnswersFromCheckedFacts() throws Exception {
        when(ai.isConfigured()).thenReturn(true);
        // Step 1: model searches. Step 2: model asks for prices. Step 3: final answer.
        when(ai.completeTurn(anyList(), any()))
                .thenReturn(new ChatCompletion("", List.of(
                                new ToolCall("c1", "search_products", "{\"query\":\"milk\"}"))))
                .thenReturn(new ChatCompletion("", List.of(
                                new ToolCall("c2", "product_prices", "{\"code\":\"P000000124\"}"))))
                .thenReturn(new ChatCompletion(
                        "The cheapest milk is 100% Fresh Milk 236mL at HK$7.50 from AEON.", List.of()));

        var reply = chat.reply("what is the cheapest milk product");

        assertEquals("answer", reply.kind());
        assertTrue(reply.answeredByModel());
        assertTrue(reply.message().contains("7.50"));
        assertTrue(reply.checkedFacts().contains("7.5000"));
    }

    @Test void agentStopsAtStepLimitAndAsksForFinalAnswer() throws Exception {
        when(ai.isConfigured()).thenReturn(true);
        when(ai.completeTurn(anyList(), argThat(tools -> tools != null)))
                .thenReturn(new ChatCompletion("", List.of(
                        new ToolCall("x", "search_products", "{\"query\":\"milk\"}"))));
        when(ai.completeTurn(anyList(), argThat(tools -> tools == null)))
                .thenReturn(new ChatCompletion("Here is what I found about milk prices.", List.of()));

        var reply = chat.reply("milk");
        assertEquals("answer", reply.kind());
        assertTrue(reply.message().contains("milk"));
    }

    @Test void unavailableAiFallsBackToCheckedRecords() throws Exception {
        when(ai.isConfigured()).thenReturn(true);
        when(ai.completeTurn(anyList(), any())).thenThrow(new AiUnavailableException("down"));

        mvc.perform(post("/ask").param("question", "100 %Fresh Milk 236mL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HK$7.5000")))
                .andExpect(content().string(containsString("AI assistant unavailable")));
    }

    @Test void unconfiguredAiStillAnswersFromCheckedRecordsAndNeverInventsAPrice() {
        when(ai.isConfigured()).thenReturn(false);
        var reply = chat.reply("100 %Fresh Milk 236mL");
        assertEquals("answer", reply.kind());
        assertFalse(reply.answeredByModel());
        assertTrue(reply.message().contains("HK$7.5000"));
    }

    @Test void askPageRendersTheChatFormAndNormalSearchStillWorksWhenAiIsDown() throws Exception {
        when(ai.isConfigured()).thenReturn(true);
        when(ai.completeTurn(anyList(), any())).thenThrow(new AiUnavailableException("down"));
        mvc.perform(get("/ask")).andExpect(status().isOk())
                .andExpect(content().string(containsString("chat-form")));
        mvc.perform(get("/").param("q", "milk")).andExpect(status().isOk())
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(not(containsString("HK$7.5 at AEON"))));
    }
}
