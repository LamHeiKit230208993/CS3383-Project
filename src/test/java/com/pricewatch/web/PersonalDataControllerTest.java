package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.preferences.DisplayPreferencesService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:personal-data;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
class PersonalDataControllerTest {
    private static final String MILK = "P000000124";
    private static final String IMPORTED_PRODUCT = "IMPORT-ONLY";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired CatalogService catalogue;
    @Autowired ShoppingService shopping;
    @Autowired TrackingService tracking;
    @Autowired TrackingRepository periods;
    @Autowired PriceObservationRepository observations;
    @Autowired AlertEventRepository alerts;
    @Autowired WatchRuleService watchRules;
    @Autowired BudgetService budget;
    @Autowired NotificationSettingsRepository notificationSettings;
    @Autowired DisplayPreferencesService display;
    @Autowired ProductRepository products;
    @MockitoBean DeepSeekChatClient chatProvider;

    @Test
    void exportsAcceptedCategoriesAndKeepsStateUntilResetCommits() throws Exception {
        catalogue.importSample();
        var accepted = catalogue.status();
        shopping.add(MILK, 2, "JASONS");
        shopping.update(MILK, 2, true, "JASONS");
        shopping.importList(new ByteArrayInputStream("""
                {"version":1,"name":"Second list","items":[{
                  "productCode":"IMPORT-ONLY","displayName":"Imported item","quantity":1,"checked":false,
                  "retailerCode":"OTHER"}]}
                """.getBytes(StandardCharsets.UTF_8)));
        tracking.save(MILK);
        tracking.startAll(MILK);
        var period = periods.findByProduct_CodeAndRetailerCodeAndEnabledTrue(MILK, "JASONS").orElseThrow();
        var now = clock.instant();
        var price = new BigDecimal("12.50");
        var observation = observations.save(new PriceObservationEntity(period, price, now, accepted.sourceUrl(),
                accepted.checksum()));
        alerts.save(new AlertEventEntity(period, observation.getId(), AlertService.TARGET, price, now,
                null, null, now, now));
        watchRules.save(MILK, null, null, LocalDateTime.ofInstant(now, ZoneId.of("Asia/Hong_Kong")).plusDays(7), "WEEKLY");
        budget.save("weekly", "200");
        display.update("en", true);
        notificationSettings.save(new NotificationSettingsEntity(LocalTime.of(23, 0), LocalTime.of(7, 0), LocalTime.of(9, 0)));

        var activeSession = new MockHttpSession();
        var otherSession = new MockHttpSession();
        when(chatProvider.reply(anyString(), anyList(), anyList(), any())).thenAnswer(invocation -> {
            BiFunction<String, JsonNode, Object> tool = invocation.getArgument(3);
            var json = new ObjectMapper();
            tool.apply("search_products", json.createObjectNode().put("query", MILK));
            tool.apply("get_product_prices", json.createObjectNode().put("product_code", MILK));
            return Optional.of("Here are the saved prices for this pack.");
        });
        mvc.perform(post("/ask").param("question", "What is the price of Kowloon Dairy 236mL?").session(activeSession))
                .andExpect(redirectedUrlPattern("/ask?chatId=*"));
        addChat(otherSession, "Compare bread");

        mvc.perform(get("/personal-data").session(activeSession))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("2 lists · 2 items")));
        mvc.perform(get("/personal-data/export").session(activeSession))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"market-desk-personal-data.json\""))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.lists", hasSize(2)))
                .andExpect(jsonPath("$.lists[0].entries[0].productCode").value(MILK))
                .andExpect(jsonPath("$.lists[0].entries[0].quantity").value(2))
                .andExpect(jsonPath("$.lists[0].entries[0].checked").value(true))
                .andExpect(jsonPath("$.lists[0].entries[0].retailerCode").value("JASONS"))
                .andExpect(jsonPath("$.lists[1].entries[0].displayName").value("Imported item"))
                .andExpect(jsonPath("$.wishlist[0].productCode").value(MILK))
                .andExpect(jsonPath("$.allRetailerTrackingProducts[0]").value(MILK))
                .andExpect(jsonPath("$.tracking[0].productCode").value(MILK))
                .andExpect(jsonPath("$.observations[0].price").value(12.5))
                .andExpect(jsonPath("$.alerts[0].type").value(AlertService.TARGET))
                .andExpect(jsonPath("$.watchRules[0].repeat").value("WEEKLY"))
                .andExpect(jsonPath("$.budgets[0].period").value("WEEKLY"))
                .andExpect(jsonPath("$.preferences.display.highContrast").value(true))
                .andExpect(jsonPath("$.preferences.notifications.summaryTime").value("09:00:00"))
                .andExpect(jsonPath("$.chats", hasSize(1)))
                .andExpect(jsonPath("$.chats[0].messages[0].text").value("What is the price of Kowloon Dairy 236mL?"))
                .andExpect(jsonPath("$.chats[0].messages[1].text").value("Here are the saved prices for this pack."))
                .andExpect(jsonPath("$.chats[0].messages[1].priceCards[0].productCode").value(MILK))
                .andExpect(jsonPath("$.chats[0].messages[1].priceCards[0].retrievedAt").value(accepted.retrievedAt().toString()))
                .andExpect(jsonPath("$.chats[0].messages[1].priceCards[0].rows[0].retailer").value("AEON"))
                .andExpect(jsonPath("$.chats[0].messages[1].priceCards[0].rows[0].price").value(7.5))
                .andExpect(jsonPath("$.catalogue").doesNotExist());

        mvc.perform(get("/personal-data/reset").session(activeSession))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Cancel")));
        mvc.perform(get("/personal-data").session(activeSession))
                .andExpect(content().string(containsString("2 lists · 2 items")));
        mvc.perform(post("/personal-data/reset").session(activeSession))
                .andExpect(redirectedUrl("/personal-data/reset"))
                .andExpect(flash().attribute("personalError", "personal.confirmRequired"));
        assertTrue(display.current().highContrast());

        jdbc.execute("create table personal_reset_blocker (product_code varchar(80) references products(code))");
        try {
            jdbc.update("insert into personal_reset_blocker (product_code) values (?)", IMPORTED_PRODUCT);
            mvc.perform(post("/personal-data/reset").param("confirm", "true").session(activeSession))
                    .andExpect(redirectedUrl("/personal-data/reset"))
                    .andExpect(flash().attribute("personalError", "personal.resetFailed"));
            assertFalse(activeSession.isInvalid());
            assertEquals(1, AskController.history(activeSession).size());
            assertEquals(2, shopping.lists().size());
            assertTrue(display.current().highContrast());
            assertTrue(products.findById(MILK).orElseThrow().isTrackAllRetailers());
        } finally {
            jdbc.execute("drop table personal_reset_blocker");
        }

        mvc.perform(post("/personal-data/reset").param("confirm", "true").session(activeSession))
                .andExpect(redirectedUrl("/personal-data"))
                .andExpect(flash().attribute("personalFeedback", "personal.resetDone"));
        assertTrue(activeSession.isInvalid());
        assertEquals(1, AskController.history(otherSession).size());
        assertFalse(products.findById(MILK).orElseThrow().isTrackAllRetailers());
        assertFalse(products.existsById(IMPORTED_PRODUCT));
        assertEquals(accepted, catalogue.status());
        assertEquals(new BigDecimal("7.5000"), catalogue.find(MILK).lowestPrice());
        mvc.perform(get("/personal-data/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lists").isEmpty())
                .andExpect(jsonPath("$.wishlist").isEmpty())
                .andExpect(jsonPath("$.allRetailerTrackingProducts").isEmpty())
                .andExpect(jsonPath("$.tracking").isEmpty())
                .andExpect(jsonPath("$.observations").isEmpty())
                .andExpect(jsonPath("$.alerts").isEmpty())
                .andExpect(jsonPath("$.reminders").isEmpty())
                .andExpect(jsonPath("$.budgets").isEmpty())
                .andExpect(jsonPath("$.preferences.display.highContrast").value(false))
                .andExpect(jsonPath("$.preferences.notifications").doesNotExist())
                .andExpect(jsonPath("$.chats").isEmpty());
    }

    private void addChat(MockHttpSession session, String question) throws Exception {
        mvc.perform(post("/ask/send").param("question", question).session(session)).andExpect(status().is3xxRedirection());
    }
}
