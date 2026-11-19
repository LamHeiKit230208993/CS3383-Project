package com.pricewatch.web;

import com.pricewatch.ask.AiAnswerClient;
import com.pricewatch.catalog.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ask;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.ai.api-key="})
@AutoConfigureMockMvc
@Transactional
@Import(AskControllerTest.FakeAi.class)
class AskControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;

    @Test
    void askPageRendersWithoutPrices() throws Exception {
        mvc.perform(get("/ask"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("Ask about a checked price")))
                .andExpect(content().string(containsString("action=\"/ask\"")));
    }

    @Test
    void noMatchExplainsThatNoCheckedRecordMatches() throws Exception {
        catalog.importSample();

        mvc.perform(post("/ask").param("question", "How much is a laptop?"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No accepted product matches that question")))
                .andExpect(content().string(containsString("How much is a laptop?")));
    }

    @Test
    void ambiguousQuestionReturnsAProductChooser() throws Exception {
        catalog.importSample();

        mvc.perform(post("/ask").param("question", "Compare bread and milk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose the product")))
                .andExpect(content().string(containsString("name=\"productCode\"")));
    }

    @Test
    void namedProductRendersCheckedFactsAndAnswer() throws Exception {
        catalog.importSample();

        mvc.perform(post("/ask")
                        .param("question", "Price for P000000124")
                        .param("productCode", "P000000124"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AI EXPLANATION OF CHECKED FACTS")))
                .andExpect(content().string(containsString("See the checked facts below.")))
                .andExpect(content().string(containsString("Checked facts")))
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(containsString("Source price date")))
                .andExpect(content().string(containsString("Unknown")));
    }

    @Test
    void blankQuestionShowsAValidationError() throws Exception {
        mvc.perform(post("/ask").param("question", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a question.")));
    }

    @TestConfiguration
    static class FakeAi {
        @Bean
        @Primary
        AiAnswerClient fakeAiAnswerClient() {
            return (Map<String, Object> facts, String question) -> "See the checked facts below.";
        }
    }
}
