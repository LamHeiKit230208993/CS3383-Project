package com.pricewatch.catalog;

import com.pricewatch.catalog.event.CatalogueReplacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:event-test;DB_CLOSE_DELAY=-1")
@Import(CatalogueEventTest.ListenerConfiguration.class)
class CatalogueEventTest {
    @Autowired
    private CatalogService catalog;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RecordingListener listener;

    @BeforeEach
    void clearRecordedEvents() {
        listener.events.clear();
    }

    @Test
    void acceptedImportPublishesOneEventAfterCommit() {
        ImportStatus accepted = catalog.importSample();

        assertEquals(1, listener.events.size());
        assertEquals(accepted.checksum(), listener.events.getFirst().status().checksum());
        assertEquals(accepted.productCount(), listener.events.getFirst().status().productCount());
    }

    @Test
    void rolledBackImportPublishesNoEvent() {
        catalog.importSample();
        listener.events.clear();

        jdbc.execute("alter table retailer_listings add constraint ck_event_failure "
                + "check (retailer_code <> 'FAIL-EVENT')");
        try {
            String replacement = "[" + product("EVENT-1", "FAIL-EVENT") + "]";
            assertThrows(DataIntegrityViolationException.class,
                    () -> catalog.accept(replacement.getBytes(StandardCharsets.UTF_8), "LIVE", "test"));

            assertEquals(0, listener.events.size());
        } finally {
            jdbc.execute("alter table retailer_listings drop constraint ck_event_failure");
        }
    }

    private String product(String code, String retailer) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"Pack","zh-Hant":"包裝","zh-Hans":"包装"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":[{"supermarketCode":"%s","price":"1.00"}],"offers":[]}
                """.formatted(code, retailer).trim();
    }

    @TestConfiguration
    static class ListenerConfiguration {
        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }

    public static class RecordingListener {
        final List<CatalogueReplacedEvent> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onCatalogueReplaced(CatalogueReplacedEvent event) {
            events.add(event);
        }
    }
}
