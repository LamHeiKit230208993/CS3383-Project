package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.budget.BudgetService;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.preferences.PreferenceRepository;
import com.pricewatch.privacy.PersonalDataService;
import com.pricewatch.privacy.PersonalDataSummary;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:privacy-web;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class PrivacyControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CatalogService catalog;
    @Autowired ShoppingService shopping;
    @Autowired TrackingService tracking;
    @Autowired BudgetService budget;
    @Autowired ReminderService reminders;
    @Autowired PersonalDataService personalData;
    @Autowired ShoppingListRepository listRepository;
    @Autowired ShoppingListItemRepository itemRepository;
    @Autowired SavedProductRepository savedRepository;
    @Autowired TrackingRepository trackingRepository;
    @Autowired PriceObservationRepository observationRepository;
    @Autowired AlertEventRepository alertEventRepository;
    @Autowired ReminderRepository reminderRepository;
    @Autowired BudgetSettingsRepository budgetRepository;
    @Autowired NotificationSettingsRepository notificationRepository;
    @Autowired PreferenceRepository preferenceRepository;
    @Autowired ProductRepository productRepository;
    @Autowired Clock clock;

    @BeforeEach
    void seedPersonalRecords() {
        alertEventRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        savedRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        reminderRepository.deleteAllInBatch();
        budgetRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        preferenceRepository.deleteAllInBatch();

        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        tracking.save("P000000124");
        tracking.startAll("P000000124");
        long periodId = tracking.trackingPeriods().getFirst().id();
        trackingRepository.findById(periodId).ifPresent(period -> observationRepository.save(
                new PriceObservationEntity(period, new BigDecimal("7.50"), clock.instant(),
                        "fixture-live", "fixture-checksum")));
        alertEventRepository.save(new AlertEventEntity(LocalDate.of(2026, 11, 19), 1, clock.instant()));
        budget.save("WEEKLY", "100");
        reminders.create("P000000124", 1, 7, "AEON");
    }

    @Test
    void privacyPageShowsPersonalRecordCountsAndControls() throws Exception {
        mvc.perform(post("/preferences").param("locale", "en").param("contrast", "high"))
                .andExpect(status().is3xxRedirection());

        PersonalDataSummary summary = personalData.summary();
        assertEquals(1, summary.lists());
        assertEquals(1, summary.listItems());
        assertEquals(1, summary.wishlist());
        assertTrue(summary.trackingPeriods() >= 1);
        assertEquals(1, summary.observations());
        assertEquals(1, summary.alerts());
        assertEquals(1, summary.reminders());
        assertTrue(summary.preferences());
        assertTrue(summary.budget());

        mvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("Privacy and your local data.")))
                .andExpect(content().string(containsString("What is stored now")))
                .andExpect(content().string(containsString("Download personal data")))
                .andExpect(content().string(containsString("Delete personal records")))
                .andExpect(content().string(containsString("I understand this permanently deletes")))
                .andExpect(content().string(containsString("data-contrast=\"high\"")));
    }

    @Test
    void exportIsVersionedPersonalDataAndOmitsTheCatalogue() throws Exception {
        mvc.perform(post("/preferences").param("locale", "zh-HK").param("contrast", "normal"))
                .andExpect(status().is3xxRedirection());

        MvcResult result = mvc.perform(get("/privacy/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("pricewatch-personal-data.json")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode root = mapper.readTree(result.getResponse().getContentAsByteArray());
        assertEquals("pricewatch-personal-data", root.path("format").asText());
        assertEquals(1, root.path("version").asInt());
        assertEquals("My list", root.path("lists").get(0).path("name").asText());
        assertEquals("P000000124", root.path("lists").get(0).path("items").get(0).path("productCode").asText());
        assertEquals("P000000124", root.path("wishlist").get(0).path("productCode").asText());
        assertEquals(1, root.path("tracking").get(0).path("observations").size());
        assertEquals(1, root.path("alerts").size());
        assertEquals("WEEKLY", root.path("budget").path("period").asText());
        assertEquals(1, root.path("reminders").size());
        assertEquals("zh-HK", root.path("preferences").path("locale").asText());
        assertFalse(root.has("products"));
        assertFalse(root.has("catalogue"));
        assertFalse(root.has("acceptedImport"));
    }

    @Test
    void deletionNeedsConfirmationAndKeepsTheAcceptedCatalogue() throws Exception {
        long productCount = productRepository.count();
        assertTrue(productRepository.findById("P000000124").orElseThrow().isTrackAllRetailers());

        mvc.perform(post("/privacy/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/privacy"))
                .andExpect(flash().attributeExists("error"));
        assertEquals(1, personalData.summary().lists(), "Nothing is deleted without confirmation.");

        mvc.perform(post("/privacy/delete").param("confirmDelete", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/privacy"))
                .andExpect(flash().attributeExists("notice"));

        PersonalDataSummary summary = personalData.summary();
        assertEquals(0, summary.lists());
        assertEquals(0, summary.listItems());
        assertEquals(0, summary.wishlist());
        assertEquals(0, summary.trackingPeriods());
        assertEquals(0, summary.observations());
        assertEquals(0, summary.alerts());
        assertEquals(0, summary.reminders());
        assertFalse(summary.preferences());
        assertFalse(summary.budget());
        assertFalse(summary.notificationSettings());

        assertEquals(productCount, productRepository.count(), "The accepted catalogue is source data.");
        ProductEntity product = productRepository.findById("P000000124").orElseThrow();
        assertNotNull(product.getName());
        assertFalse(product.isTrackAllRetailers(), "Personal tracking choices are reset.");
        assertNotNull(catalog.status());
        assertTrue(catalog.find("P000000124").listings().size() > 0);
    }
}
