package com.pricewatch.notifications.persistence;

import org.springframework.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@DependsOn("entityManagerFactory")
class WatchRuleSchema implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(WatchRuleSchema.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    WatchRuleSchema(JdbcTemplate jdbc, TransactionTemplate transaction) {
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Override
    public void afterPropertiesSet() {
        // Hibernate adds columns on upgrade but leaves earlier NOT NULL constraints in place.
        allowNull("WATCH_RULES", "TARGET");
        allowNull("PRODUCT_TARGET_EVENTS", "TARGET");
        allowNull("PRODUCT_TARGET_EVENTS", "RETRIEVED_AT");
        var counts = transaction.execute(status -> migrateReminders());
        if (counts != null && (counts.rules() != 0 || counts.occurrences() != 0)) {
            LOG.info("Migrated {} reminder rules and {} reminder occurrences.", counts.rules(), counts.occurrences());
        }
    }

    private MigrationCounts migrateReminders() {
        boolean rulesExist = exists("REPLENISHMENT_REMINDERS");
        boolean historyExists = exists("REMINDER_OCCURRENCES");
        int rules = 0;
        int occurrences = 0;
        if (rulesExist) {
            // The J05–J08 version predates the pause switch.
            String paused = hasColumn("REPLENISHMENT_REMINDERS", "PAUSED") ? "old.paused" : "null";
            jdbc.update("""
                    insert into watch_rules (product_code, interval_days, next_due_at, armed,
                        waiting_for_live, paused, legacy_reminder_id, cart_quantity, cart_retailer, snoozed_until)
                    select old.product_code, old.interval_days, old.next_due_at, true, false, %s,
                        old.id, old.quantity, old.selected_retailer_code, old.snoozed_until
                    from replenishment_reminders old
                    where not exists (select 1 from watch_rules rule where rule.legacy_reminder_id = old.id)
                    """.formatted(paused));
        }
        if (historyExists) {
            jdbc.update("""
                    insert into product_target_events (product_code, created_at, read_at, snoozed_until,
                        rule_id, scheduled_due_at, legacy_occurrence_id, cart_quantity, cart_retailer)
                    select old.product_code, old.created_at, old.read_at, old.snoozed_until, rule.id,
                        old.due_at, old.id, old.quantity, old.retailer_code
                    from reminder_occurrences old
                    left join watch_rules rule on rule.legacy_reminder_id = old.reminder_id
                    where not exists (select 1 from product_target_events event where event.legacy_occurrence_id = old.id)
                    """);
            occurrences = jdbc.update("delete from reminder_occurrences");
        }
        if (rulesExist) rules = jdbc.update("delete from replenishment_reminders");
        return new MigrationCounts(rules, occurrences);
    }

    private record MigrationCounts(int rules, int occurrences) {}

    private boolean exists(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from information_schema.tables
                    where table_schema = current_schema() and table_name = ?)
                """, Boolean.class, table));
    }

    private boolean hasColumn(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from information_schema.columns
                    where table_schema = current_schema() and table_name = ? and column_name = ?)
                """, Boolean.class, table, column));
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
