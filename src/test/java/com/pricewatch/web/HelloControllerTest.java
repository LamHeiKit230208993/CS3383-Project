package com.pricewatch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootReturnsMarketDeskView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Market Desk · PriceWatch</title>")));
    }

    @Test
    void rootIncludesPrototypeScenesAndLocalAssets() throws Exception {
        var response = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UI prototype")))
                .andExpect(content().string(containsString("Sample data")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<link\\b[^>]*href=\"/css/market-desk\\.css\"[^>]*>.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<script\\b(?=[^>]*src=\"/js/market-desk\\.js\")(?=[^>]*\\bdefer\\b)[^>]*>.*")));

        for (String scene : new String[]{"discover", "compare", "list", "saved", "updates", "assistant"}) {
            response.andExpect(content().string(matchesPattern(
                    "(?s).*<section\\b[^>]*id=\"scene-" + scene + "\"[^>]*>.*")));
        }
    }

    @Test
    void rootProvidesNamedSearchAndReachableDemoControls() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(
                        "(?s).*<label\\b[^>]*for=\"product-search\"[^>]*>\\s*[^<\\s].*?</label>.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<input\\b[^>]*id=\"product-search\"[^>]*>.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<main\\b[^>]*id=\"main-content\"[^>]*>.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<nav\\b[^>]*>.*?<a\\b[^>]*href=\"#discover\"[^>]*>.*?</nav>.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<dialog\\b[^>]*id=\"settings-dialog\"[^>]*>.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<dialog\\b[^>]*id=\"quantity-dialog\"[^>]*>.*")));
    }
}
