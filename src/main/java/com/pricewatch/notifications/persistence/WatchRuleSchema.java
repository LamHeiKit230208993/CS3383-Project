package com.pricewatch.notifications.persistence;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("entityManagerFactory")
class WatchRuleSchema implements InitializingBean {
    private final JdbcTemplate jdbc;

    WatchRuleSchema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterPropertiesSet() {
        // Hibernate adds columns on upgrade but leaves earlier NOT NULL constraints in place.
        allowNull("WATCH_RULES", "TARGET");
        allowNull("PRODUCT_TARGET_EVENTS", "TARGET");
        allowNull("PRODUCT_TARGET_EVENTS", "RETRIEVED_AT");
    }

    private void allowNull(String table, String column) {
        Boolean required = jdbc.queryForObject("""
                select is_nullable = 'NO' from information_schema.columns
                where table_schema = current_schema() and table_name = ? and column_name = ?
                """, Boolean.class, table, column);
        if (Boolean.TRUE.equals(required)) {
            jdbc.execute("alter table " + table + " alter column " + column + " drop not null");
        }
    }
}
