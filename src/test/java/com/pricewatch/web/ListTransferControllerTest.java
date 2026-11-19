package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.sharing.ListPreview;
import com.pricewatch.sharing.ListTransferService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:share-web;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class ListTransferControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired ShoppingService shopping;
    @Autowired ListTransferService transfer;
    @Autowired ObjectMapper mapper;
    @Autowired ShoppingListRepository listRepository;
    @Autowired ShoppingListItemRepository itemRepository;

    @BeforeEach
    void reset() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        catalog.importSample();
    }

    @Test
    void transferPageExplainsSnapshotAndShowsTheUploadForm() throws Exception {
        mvc.perform(get("/list/import"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("Export or import a list")))
                .andExpect(content().string(containsString("action=\"/list/import\"")))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("never overwrites or merges")))
                .andExpect(content().string(containsString("Limit: 2 MB")));
    }

    @Test
    void exportingTheMainListReturnsAVersionedAttachment() throws Exception {
        shopping.add("P000000124", 2);

        MvcResult result = mvc.perform(get("/list/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("pricewatch.json")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode root = mapper.readTree(result.getResponse().getContentAsByteArray());
        assertEquals("pricewatch-list", root.path("format").asText());
        assertEquals(1, root.path("version").asInt());
        assertEquals("P000000124", root.path("items").get(0).path("productCode").asText());
        assertEquals(2, root.path("items").get(0).path("quantity").asInt());
    }

    @Test
    void offlineSnapshotIsASelfContainedHtmlAttachmentWithSavedAmounts() throws Exception {
        shopping.add("P000000124", 2, "JASONS");
        shopping.update("P000000124", 2, true, "JASONS");

        MvcResult result = mvc.perform(get("/list/offline"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".pricewatch.html")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<!DOCTYPE html>")))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(containsString("JASONS")))
                .andExpect(content().string(containsString("HK$12.50")))
                .andExpect(content().string(containsString("HK$25.00")))
                .andExpect(content().string(containsString("Data age")))
                .andExpect(content().string(containsString("Source price date")))
                .andExpect(content().string(containsString("Unknown")))
                .andExpect(content().string(containsString("Read-only")))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<form"));
        assertFalse(html.contains("<a "));
        assertFalse(html.contains("href="));
        assertFalse(html.contains("src="));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));
        assertFalse(html.contains("url("));
    }

    @Test
    void offlineSnapshotCanDescribeAChosenListAndUnknownListIs404() throws Exception {
        ListPreview imported = transfer.importFile(
                file("<b>Trip</b>", item("P000000229", 4, false, null)));

        MvcResult result = mvc.perform(get("/list/offline").param("listId", imported.id().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;b&gt;Trip&lt;/b&gt;")))
                .andExpect(content().string(containsString("P000000229")))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .contains("<b>Trip</b>"));
        mvc.perform(get("/list/offline").param("listId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transferPageLinksToTheOfflineSnapshotForEveryList() throws Exception {
        shopping.add("P000000124", 1);
        transfer.importFile(file("Trip", item("P000000229", 1, false, null)));

        mvc.perform(get("/list/import"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/list/offline?listId=")))
                .andExpect(content().string(containsString("Offline copy")))
                .andExpect(content().string(containsString("self-contained HTML file")));
    }

    @Test
    void validUploadCreatesASeparateListAndShowsItsPreview() throws Exception {
        shopping.add("P000000229", 1);
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mvc.perform(multipart("/list/import")
                        .file(upload("trip.json", file("Trip", item("P000000124", 3, true, "JASONS"))))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertNotNull(location);
        mvc.perform(get(location).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Imported Trip as a new list.")))
                .andExpect(content().string(containsString("Imported list")))
                .andExpect(content().string(containsString("Separate list")))
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(containsString("JASONS")));
        assertEquals(2, transfer.lists().size());
        assertEquals(1, transfer.lists().get(1).itemCount());
    }

    @Test
    void invalidUploadShowsTheValidationErrorAndCreatesNothing() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(multipart("/list/import")
                        .file(upload("broken.json", "{not json".getBytes(StandardCharsets.UTF_8)))
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/list/import"));

        mvc.perform(get("/list/import").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("not a readable list file")));
        assertEquals(0, listRepository.count());
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(multipart("/list/import")
                        .file(new MockMultipartFile("file", "empty.json", "application/json", new byte[0]))
                        .session(session))
                .andExpect(redirectedUrl("/list/import"));

        mvc.perform(get("/list/import").session(session))
                .andExpect(content().string(containsString("Choose a list file to import.")));
        assertEquals(0, listRepository.count());
    }

    @Test
    void oversizedFileIsRejectedByTheService() throws Exception {
        MockHttpSession session = new MockHttpSession();
        byte[] tooLarge = new byte[ListTransferService.MAX_BYTES + 1];

        mvc.perform(multipart("/list/import")
                        .file(upload("large.json", tooLarge))
                        .session(session))
                .andExpect(redirectedUrl("/list/import"));

        mvc.perform(get("/list/import").session(session))
                .andExpect(content().string(containsString("larger than 2 MB")));
        assertEquals(0, listRepository.count());
    }

    @Test
    void anySavedListCanBePreviewedOrExported() throws Exception {
        ListPreview imported = transfer.importFile(
                file("Trip", item("P000000229", 4, false, null)));

        mvc.perform(get("/list/import").param("listId", imported.id().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trip")))
                .andExpect(content().string(containsString("P000000229")));

        MvcResult result = mvc.perform(get("/list/export").param("listId", imported.id().toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = mapper.readTree(result.getResponse().getContentAsByteArray());
        assertEquals("Trip", root.path("name").asText());
    }

    @Test
    void unknownListIsReportedGracefully() throws Exception {
        mvc.perform(get("/list/import").param("listId", "999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That list was not found.")));

        mvc.perform(get("/list/export").param("listId", "999"))
                .andExpect(status().isNotFound());
    }

    private MockMultipartFile upload(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/json", content);
    }

    private byte[] file(String name, String... items) {
        String json = "{\"format\":\"pricewatch-list\",\"version\":1,"
                + "\"exportedAt\":\"2026-11-19T00:00:00Z\","
                + "\"name\":\"" + name + "\",\"items\":[" + String.join(",", items) + "]}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String item(String code, int quantity, boolean checked, String retailer) {
        return "{\"productCode\":\"" + code + "\",\"quantity\":" + quantity
                + ",\"checked\":" + checked
                + ",\"retailerCode\":" + (retailer == null ? "null" : "\"" + retailer + "\"") + "}";
    }
}
