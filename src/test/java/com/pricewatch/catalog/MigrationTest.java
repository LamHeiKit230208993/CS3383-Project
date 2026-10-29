package com.pricewatch.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:migration-test;DB_CLOSE_DELAY=-1")
class MigrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void baselineMigrationIsAppliedAndSchemaIsValidated() {
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from \"flyway_schema_history\" "
                        + "where \"version\" = '1' and \"success\" = true",
                Integer.class));

        for (String table : List.of("PRODUCTS", "RETAILER_LISTINGS", "RETAILER_OFFERS",
                "ACCEPTED_IMPORT", "SHOPPING_LISTS", "SHOPPING_LIST_ITEMS")) {
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_name = ?",
                    Integer.class, table), table);
        }

        for (String constraint : List.of("UK_LISTING_PRODUCT_RETAILER", "UK_LIST_ITEM_PRODUCT")) {
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from information_schema.table_constraints where constraint_name = ?",
                    Integer.class, constraint), constraint);
        }
    }
}
