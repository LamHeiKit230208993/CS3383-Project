package com.pricewatch.reminder;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReminderPersistenceRestartTest {
    @Test
    void reminderOccurrenceSnoozeAndPauseSurviveClosingAndReopeningTheApplication(@TempDir Path directory) {
        String databaseUrl = "jdbc:h2:file:" + directory.resolve("reminder-restart").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        Long reminderId;
        long occurrenceId;
        Instant now = Instant.parse("2026-11-06T02:00:00Z");
        Instant due = now.plus(21, ChronoUnit.DAYS);

        try (ConfigurableApplicationContext firstRun = start(databaseUrl, now)) {
            firstRun.getBean(CatalogService.class).importSample();
            ReminderService service = firstRun.getBean(ReminderService.class);
            service.create("P000000124", 4, 21, "JASONS");
            ReminderView created = service.overview().reminders().getFirst();
            reminderId = created.id();
            assertEquals(due, created.nextDueAt());
            assertTrue(service.occurrences(now).isEmpty());
            var jdbc = firstRun.getBean(JdbcTemplate.class);
            jdbc.execute("DROP TABLE reminder_occurrences");
            jdbc.execute("ALTER TABLE replenishment_reminders DROP COLUMN paused");
        }

        try (ConfigurableApplicationContext atDue = start(databaseUrl, due)) {
            ReminderService service = atDue.getBean(ReminderService.class);
            assertTrue(service.overview().reminders().getFirst().enabled());
            service.recordDue();
            occurrenceId = service.occurrences(due).getFirst().id();
            service.snoozeOccurrence(occurrenceId);
            service.setEnabled(reminderId, false);
        }

        try (ConfigurableApplicationContext reopened = start(databaseUrl, due)) {
            ReminderService service = reopened.getBean(ReminderService.class);
            ReminderView restored = service.overview().reminders().getFirst();
            assertEquals(reminderId, restored.id());
            assertEquals("P000000124", restored.productCode());
            assertEquals(4, restored.quantity());
            assertEquals(21, restored.intervalDays());
            assertEquals("JASONS", restored.selectedRetailerCode());
            assertFalse(restored.enabled());
            assertEquals(due.plusSeconds(24 * 60 * 60L), restored.effectiveDueAt());
            assertNotNull(reopened.getBean(ReminderRepository.class).findWithProductById(reminderId).orElse(null));
            service.recordDue();
            var events = service.occurrences(due);
            assertEquals(1, events.size());
            assertEquals(occurrenceId, events.getFirst().id());
            assertFalse(events.getFirst().available());
            assertFalse(events.getFirst().active());
            service.setEnabled(reminderId, true);
            assertTrue(service.occurrences(due).getFirst().active());
        }
    }

    private ConfigurableApplicationContext start(String databaseUrl, Instant now) {
        return new SpringApplicationBuilder(PricewatchApplication.class)
                .web(WebApplicationType.NONE)
                .initializers(context -> ((GenericApplicationContext) context).registerBean("reminderFixtureClock",
                        Clock.class, () -> Clock.fixed(now, ZoneOffset.UTC), bean -> bean.setPrimary(true)))
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
