package com.pricewatch.catalog;

import com.pricewatch.PricewatchApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MigrationLegacyBaselineTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void existingSchemaIsBaselinedWithoutLosingData() {
        String jdbcUrl = "jdbc:h2:file:" + temporaryDirectory.resolve("legacy-db");

        try (ConfigurableApplicationContext firstRun = start(jdbcUrl, false, "create")) {
            firstRun.getBean(JdbcTemplate.class)
                    .update("insert into products(code, active) values ('LEGACY', true)");
        }

        try (ConfigurableApplicationContext reopened = start(jdbcUrl, true, "validate")) {
            JdbcTemplate jdbc = reopened.getBean(JdbcTemplate.class);
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from \"flyway_schema_history\" "
                            + "where upper(\"type\") = 'BASELINE' and \"version\" = '1' and \"success\" = true",
                    Integer.class));
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from products where code = 'LEGACY'", Integer.class));
        }
    }

    private ConfigurableApplicationContext start(String jdbcUrl, boolean flyway, String ddlAuto) {
        return new SpringApplicationBuilder(PricewatchApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.flyway.enabled=" + flyway,
                        "--spring.flyway.baseline-on-migrate=true",
                        "--spring.flyway.baseline-version=1",
                        "--spring.jpa.hibernate.ddl-auto=" + ddlAuto,
                        "--spring.main.banner-mode=off",
                        "--logging.level.root=ERROR");
    }
}
