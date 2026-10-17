package com.pricewatch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(PageController.class)
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRedirectsToList() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/list"));
    }

    @Test
    void eachScreenResolvesToItsTemplate() throws Exception {
        mockMvc.perform(get("/search")).andExpect(status().isOk()).andExpect(view().name("search"));
        mockMvc.perform(get("/assistant")).andExpect(status().isOk()).andExpect(view().name("assistant"));
        mockMvc.perform(get("/alerts")).andExpect(status().isOk()).andExpect(view().name("alerts"));
        mockMvc.perform(get("/list")).andExpect(status().isOk()).andExpect(view().name("list"));
        mockMvc.perform(get("/history")).andExpect(status().isOk()).andExpect(view().name("history"));
        mockMvc.perform(get("/more")).andExpect(status().isOk()).andExpect(view().name("more"));
    }

    @Test
    void searchIncludesTheComparisonCardLink() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a class=\"compare\"")))
                .andExpect(content().string(containsString("href=\"/history\"")));
    }

    @Test
    void pagesLabelSampleDataAndLinkToHistory() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sample data")))
                .andExpect(content().string(containsString("href=\"/history\"")));
    }
}