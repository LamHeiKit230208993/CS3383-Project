package com.pricewatch.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PrototypeController.class)
class PrototypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRedirectsToSearch() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search"));
    }

    @Test
    void everyPrototypeScreenResolvesToItsTemplate() throws Exception {
        List<Route> routes = List.of(
                new Route("/search", "search"),
                new Route("/compare", "compare"),
                new Route("/list", "list"),
                new Route("/saved", "saved"),
                new Route("/alerts", "alerts"),
                new Route("/history", "history"),
                new Route("/assistant", "assistant"),
                new Route("/settings", "settings")
        );

        for (Route route : routes) {
            mockMvc.perform(get(route.path()))
                    .andExpect(status().isOk())
                    .andExpect(view().name(route.view()))
                    .andExpect(content().string(containsString("Sample data")));
        }
    }

    @Test
    void searchPageHasLabelledInputAndCompareLink() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("for=\"product-search\"")))
                .andExpect(content().string(containsString("id=\"product-search\"")))
                .andExpect(content().string(containsString("href=\"/compare\"")))
                .andExpect(content().string(containsString("id=\"no-results\"")));
    }

    @Test
    void comparePageShowsProductRetailerAndMissingStates() throws Exception {
        mockMvc.perform(get("/compare"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("P000000192")))
                .andExpect(content().string(containsString("AEON")))
                .andExpect(content().string(containsString("TIED")))
                .andExpect(content().string(containsString("NO LISTING")))
                .andExpect(content().string(containsString("Source date")));
    }

    @Test
    void comparePageCanShowAnotherSampleProduct() throws Exception {
        mockMvc.perform(get("/compare").param("product", "P000000311"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rice 5kg")))
                .andExpect(content().string(containsString("P000000311")))
                .andExpect(content().string(containsString("$4.00 below the next cheapest sample listing")));
    }

    @Test
    void listPageShowsQuantitiesAndEstimatedTotal() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-quantity")))
                .andExpect(content().string(containsString("Estimated total")))
                .andExpect(content().string(containsString("Not actual spending")));
    }

    @Test
    void savedPageSeparatesWishlistAndTracking() throws Exception {
        mockMvc.perform(get("/saved"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Wishlist")))
                .andExpect(content().string(containsString("Tracking")))
                .andExpect(content().string(containsString("Saving an item does not start tracking")));
    }

    @Test
    void alertsPageShowsTargetsQuietHoursAndSnooze() throws Exception {
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Target: below $45.00")))
                .andExpect(content().string(containsString("Quiet hours")))
                .andExpect(content().string(containsString("Snooze 3 months")))
                .andExpect(content().string(containsString("in-app only")));
    }

    @Test
    void historyPageShowsAvailableDatesAndEmptyState() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("30-day sample history")))
                .andExpect(content().string(containsString("Not enough history yet")))
                .andExpect(content().string(containsString("not a prediction")));
    }

    @Test
    void assistantPageShowsCheckedSampleComparison() throws Exception {
        mockMvc.perform(get("/assistant"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sample conversation")))
                .andExpect(content().string(containsString("checked sample comparison")))
                .andExpect(content().string(containsString("No AI service is called")));
    }

    @Test
    void settingsPageShowsLanguageAndContrastControls() throws Exception {
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("High-contrast mode")))
                .andExpect(content().string(containsString("data-lang=\"zh-Hant\"")))
                .andExpect(content().string(containsString("Local-only prototype")));
    }

    @Test
    void prototypeAssetsDoNotLoadExternalResources() throws IOException {
        List<Path> roots = List.of(
                Path.of("src/main/resources/templates"),
                Path.of("src/main/resources/static")
        );

        for (Path root : roots) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) {
                    String content = Files.readString(file)
                            .replace("http://www.thymeleaf.org", "");
                    if (content.contains("http://") || content.contains("https://")) {
                        throw new AssertionError("External URL found in " + file);
                    }
                }
            }
        }
    }

    private record Route(String path, String view) {
    }
}
