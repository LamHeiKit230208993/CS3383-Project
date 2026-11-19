package com.pricewatch.web;

import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.preferences.PreferenceRepository;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.TrackingView;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-history-web;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"})
@AutoConfigureMockMvc
class ProductHistoryControllerTest {
    private static final String CODE = "P000000124";
    private static final Instant START = Instant.parse("2026-07-01T04:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired TrackingService tracking;
    @Autowired TrackingRepository trackingRepository;
    @Autowired PriceObservationRepository observationRepository;
    @Autowired SavedProductRepository savedRepository;
    @Autowired AlertEventRepository alertEventRepository;
    @Autowired ShoppingListItemRepository itemRepository;
    @Autowired ShoppingListRepository listRepository;
    @Autowired ReminderRepository reminderRepository;
    @Autowired BudgetSettingsRepository budgetRepository;
    @Autowired NotificationSettingsRepository notificationRepository;
    @Autowired PreferenceRepository preferenceRepository;
    @Autowired AcceptedImportRepository acceptedImportRepository;
    @Autowired OfferRepository offerRepository;
    @Autowired RetailerListingRepository listingRepository;
    @Autowired ProductRepository productRepository;

    @MockitoBean Clock clock;

    private final AtomicReference<Instant> now = new AtomicReference<>(START);

    @BeforeEach
    void resetDataAndClock() {
        now.set(START);
        when(clock.instant()).thenAnswer(invocation -> now.get());

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
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        acceptedImportRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        catalog.importSample();
    }

    @Test
    void historyIsUnavailableUntilTheProductIsTracked() throws Exception {
        mvc.perform(get("/history/{code}", CODE))
                .andExpect(status().isNotFound());

        mvc.perform(get("/history/{code}", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyShowsRealObservationDatesAndExplicitGaps() throws Exception {
        TrackingView period = tracking.start(CODE, firstRetailer());
        observation(period, "12.34", START.plus(Duration.ofHours(1)));
        observation(period, "11.00", START.plus(Duration.ofDays(11)));
        observation(period, "10.50", START.plus(Duration.ofDays(12)));
        now.set(START.plus(Duration.ofDays(12)).plus(Duration.ofHours(1)));

        mvc.perform(get("/history/{code}", CODE))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("History for")))
                .andExpect(content().string(containsString("Tracked retailers")))
                .andExpect(content().string(containsString(firstRetailer())))
                .andExpect(content().string(containsString("2 Jul 2026")))
                .andExpect(content().string(containsString("12 Jul 2026")))
                .andExpect(content().string(containsString("13 Jul 2026")))
                .andExpect(content().string(containsString("HK$12.34")))
                .andExpect(content().string(containsString("HK$11.00")))
                .andExpect(content().string(containsString("HK$10.50")))
                .andExpect(content().string(containsString("10 days with no recorded observation")))
                .andExpect(content().string(containsString("daily values are not invented")));
    }

    @Test
    void historyShowsAnEmptyStateWhenOnlyExpiredObservationsExist() throws Exception {
        TrackingView period = tracking.start(CODE, firstRetailer());
        observation(period, "99.99", START.minus(Duration.ofDays(100)));
        now.set(START.plus(Duration.ofDays(1)));

        mvc.perform(get("/history/{code}", CODE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No recorded observations.")))
                .andExpect(content().string(containsString("no accepted live import has recorded a price")))
                .andExpect(content().string(not(containsString("HK$99.99"))));
    }

    @Test
    void historyDropsObservationsOlderThanNinetyDays() throws Exception {
        TrackingView period = tracking.start(CODE, firstRetailer());
        observation(period, "99.99", START.minus(Duration.ofDays(100)));
        observation(period, "7.77", START.plus(Duration.ofDays(1)));
        now.set(START.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));

        mvc.perform(get("/history/{code}", CODE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HK$7.77")))
                .andExpect(content().string(not(containsString("HK$99.99"))))
                .andExpect(content().string(not(containsString("23 Mar 2026"))));
    }

    @Test
    void savedPageLinksToHistoryForTrackedProducts() throws Exception {
        tracking.start(CODE, firstRetailer());

        mvc.perform(get("/saved"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("View history")))
                .andExpect(content().string(containsString("href=\"/history/" + CODE + "\"")));
    }

    private String firstRetailer() {
        return catalog.find(CODE).listings().getFirst().retailerCode();
    }

    private void observation(TrackingView period, String price, Instant observedAt) {
        observationRepository.save(new PriceObservationEntity(
                trackingRepository.findById(period.id()).orElseThrow(),
                new BigDecimal(price), observedAt, "fixture-live", "fixture-checksum"));
    }
}
