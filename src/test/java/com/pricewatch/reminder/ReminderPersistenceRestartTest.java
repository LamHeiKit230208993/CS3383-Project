package com.pricewatch.reminder;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReminderPersistenceRestartTest {
    @Test
    void reminderAndSnoozeSurviveClosingAndReopeningTheApplication(@TempDir Path directory) {
        String databaseUrl = "jdbc:h2:file:" + directory.resolve("reminder-restart").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        Long reminderId;
        Instant initialDue;

        try (ConfigurableApplicationContext firstRun = start(databaseUrl)) {
            firstRun.getBean(CatalogService.class).importSample();
            ReminderService service = firstRun.getBean(ReminderService.class);
            service.create("P000000124", 4, 21, "JASONS");
            ReminderView created = service.overview().reminders().getFirst();
            reminderId = created.id();
            initialDue = created.nextDueAt();
            service.snooze(reminderId);
        }

        try (ConfigurableApplicationContext reopened = start(databaseUrl)) {
            ReminderView restored = reopened.getBean(ReminderService.class).overview().reminders().getFirst();
            assertEquals(reminderId, restored.id());
            assertEquals("P000000124", restored.productCode());
            assertEquals(4, restored.quantity());
            assertEquals(21, restored.intervalDays());
            assertEquals("JASONS", restored.selectedRetailerCode());
            assertEquals(initialDue.plusSeconds(24 * 60 * 60L), restored.effectiveDueAt());
            assertNotNull(reopened.getBean(ReminderRepository.class).findWithProductById(reminderId).orElse(null));
        }
    }

    private ConfigurableApplicationContext start(String databaseUrl) {
        return new SpringApplicationBuilder(PricewatchApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + databaseUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.jpa.open-in-view=false",
                        "--spring.main.banner-mode=off",
                        "--spring.main.web-application-type=none",
                        "--pricewatch.jobs.enabled=false");
    }
}
