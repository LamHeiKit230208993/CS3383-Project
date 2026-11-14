package com.pricewatch.budget;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetSettingsPersistenceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savedPeriodAndLimitSurviveReopeningTheH2File() {
        String jdbcUrl = "jdbc:h2:file:" + temporaryDirectory.resolve("budget-reopen-db");

        try (ConfigurableApplicationContext firstRun = start(jdbcUrl)) {
            firstRun.getBean(BudgetSettingsRepository.class).saveAndFlush(
                    new BudgetSettingsEntity(BudgetPeriod.MONTHLY, new BigDecimal("1250.7500")));
        }

        try (ConfigurableApplicationContext reopened = start(jdbcUrl)) {
            var restored = reopened.getBean(BudgetSettingsRepository.class).findById(1L).orElseThrow();
            assertEquals(BudgetPeriod.MONTHLY, restored.getPeriod());
            assertEquals(new BigDecimal("1250.7500"), restored.getLimitAmount());
            assertEquals(1L, reopened.getBean(BudgetSettingsRepository.class).count());
        }
    }

    private ConfigurableApplicationContext start(String jdbcUrl) {
        return new SpringApplicationBuilder(PricewatchApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.main.banner-mode=off",
                        "--logging.level.root=ERROR");
    }
}
