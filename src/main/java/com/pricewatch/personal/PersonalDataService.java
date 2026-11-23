package com.pricewatch.personal;

import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.notifications.persistence.ProductTargetEventEntity;
import com.pricewatch.notifications.persistence.ProductTargetRuleEntity;
import com.pricewatch.notifications.persistence.WatchRuleEntity;
import com.pricewatch.preferences.DisplayPreferencesService;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.SavedProductEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Service
public class PersonalDataService {
    private final EntityManager entities;
    private final DisplayPreferencesService displayPreferences;
    private final ProductTargetService priceTargets;
    private final WatchRuleService watchRules;

    public PersonalDataService(EntityManager entities, DisplayPreferencesService displayPreferences,
                               ProductTargetService priceTargets, WatchRuleService watchRules) {
        this.entities = entities;
        this.displayPreferences = displayPreferences;
        this.priceTargets = priceTargets;
        this.watchRules = watchRules;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Overview overview() {
        return new Overview(count(ShoppingListEntity.class), count(ShoppingListItemEntity.class),
                count(SavedProductEntity.class), count(TrackingEntity.class), count(PriceObservationEntity.class),
                count(AlertEventEntity.class) + count(ProductTargetEventEntity.class),
                entities.createQuery("select count(rule) from WatchRuleEntity rule where rule.target is null", Long.class)
                        .getSingleResult(),
                count(BudgetSettingsEntity.class));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PersonalDataExport export(List<PersonalDataExport.Chat> chats, Locale locale) {
        var wishlist = rows(SavedProductEntity.class, "productCode").stream()
                .map(entry -> new PersonalDataExport.WishlistEntry(entry.getProductCode(), entry.getSavedAt())).toList();
        var productsTrackedAtAllRetailers = entities.createQuery("""
                select product.code from ProductEntity product
                where product.trackAllRetailers = true order by product.code
                """, String.class).getResultList();
        var tracking = rows(TrackingEntity.class, "id").stream()
                .map(row -> new PersonalDataExport.TrackedRetailer(row.getId(), row.getProductCode(),
                        row.getRetailerCode(), row.isEnabled(), row.getStartedAt(), row.getStoppedAt(),
                        row.getTargetPrice(), row.isTargetArmed(), row.isDropEnabled(), row.isAlertsEnabled())).toList();
        var observations = rows(PriceObservationEntity.class, "id").stream()
                .map(row -> new PersonalDataExport.Observation(row.getId(), row.getTracking().getId(),
                        row.getPrice(), row.getObservedAt())).toList();
        var alerts = rows(AlertEventEntity.class, "id").stream()
                .map(this::alert).toList();
        var watchRows = watchRules.allRules();
        var reminders = watchRows.stream().filter(rule -> rule.cartQuantity() != null)
                .map(rule -> new PersonalDataExport.Reminder(rule.id(), rule.productCode(), rule.cartQuantity(),
                        rule.intervalDays(), rule.cartRetailer(), rule.nextDueAt(),
                        rule.snoozedUntil(), rule.enabled())).toList();
        var budgets = rows(BudgetSettingsEntity.class, "id").stream()
                .map(row -> new PersonalDataExport.Budget(row.getPeriod().name(), row.getLimitAmount())).toList();
        var targets = rows(ProductTargetRuleEntity.class, "productCode").stream()
                .map(row -> new PersonalDataExport.PriceTarget(row.getProductCode(), row.getTarget(), row.isArmed(),
                        row.isEnabled())).toList();
        var eventRows = rows(ProductTargetEventEntity.class, "id");
        var priceAlerts = eventRows.stream().filter(row -> row.getTarget() != null).map(this::productPriceAlert).toList();
        var timeAlerts = eventRows.stream().filter(row -> row.getTarget() == null)
                .map(row -> new PersonalDataExport.TimeAlert(row.getId(), row.getRuleId(), row.getProductCode(),
                        row.getCartQuantity(), row.getCartRetailer(), row.getScheduledDueAt(), row.getCreatedAt(), row.getReadAt(),
                        row.getSnoozedUntil())).toList();
        var rules = watchRows.stream()
                .map(rule -> new PersonalDataExport.WatchRule(rule.id(), rule.productCode(), rule.target(),
                        rule.intervalDays(), rule.nextDueAt(), rule.armed(), rule.waitingForLive(),
                        rule.enabled(), rule.repeat(), rule.startAt(), rule.completed(), rule.priceKind())).toList();
        return new PersonalDataExport(1, lists(locale), wishlist, productsTrackedAtAllRetailers,
                tracking, observations, alerts, reminders, budgets, preferences(), List.copyOf(chats),
                targets, priceAlerts, timeAlerts, rules);
    }

    @Transactional
    public void reset() {
        entities.createQuery("""
                update ProductEntity product set product.trackAllRetailers = false
                where product.trackAllRetailers = true
                """).executeUpdate();
        delete(AlertEventEntity.class);
        watchRules.reset();
        priceTargets.reset();
        delete(PriceObservationEntity.class);
        delete(TrackingEntity.class);
        delete(SavedProductEntity.class);
        delete(ShoppingListItemEntity.class);
        delete(ShoppingListEntity.class);
        delete(BudgetSettingsEntity.class);
        delete(NotificationSettingsEntity.class);
        displayPreferences.reset();
        // List imports can create these records before a source catalogue contains the product.
        entities.createQuery("""
                delete from ProductEntity product
                where product.importedListReference = true and product.active = false
                """).executeUpdate();
    }

    private List<PersonalDataExport.ShoppingList> lists(Locale locale) {
        var entriesByList = new HashMap<Long, List<PersonalDataExport.ListEntry>>();
        var itemRows = entities.createQuery("""
                select item.shoppingList.id, product.code, product.name, item.quantity, item.checked, item.selectedRetailerCode
                from ShoppingListItemEntity item join item.product product order by item.id
                """, ListRow.class).getResultList();
        for (var row : itemRows) {
            entriesByList.computeIfAbsent(row.listId(), id -> new ArrayList<>()).add(
                    new PersonalDataExport.ListEntry(row.productCode(), name(row, locale),
                            row.quantity(), row.checked(), row.retailerCode()));
        }
        return rows(ShoppingListEntity.class, "id").stream()
                .map(list -> new PersonalDataExport.ShoppingList(list.getId(), list.getName(),
                        List.copyOf(entriesByList.getOrDefault(list.getId(), List.of()))))
                .toList();
    }

    private static String name(ListRow row, Locale locale) {
        var names = row.name();
        if (names == null) return row.productCode();
        var name = new LocalizedText(names.getEn(), names.getZhHant(), names.getZhHans()).forLocale(locale);
        return name.isBlank() ? row.productCode() : name;
    }

    private PersonalDataExport.Alert alert(AlertEventEntity row) {
        Long trackingId = row.getTracking() == null ? null : row.getTracking().getId();
        return new PersonalDataExport.Alert(row.getId(), trackingId, row.getObservationId(), row.getType(),
                row.getPrice(), row.getObservedAt(), row.getPreviousPrice(), row.getPreviousObservedAt(),
                row.getCreatedAt(), row.getAvailableAt(), row.getReadAt(), row.getSnoozedUntil(),
                row.getSummaryDate(), row.getSummaryCount(), row.getSummarizedAt());
    }

    private PersonalDataExport.ProductPriceAlert productPriceAlert(ProductTargetEventEntity row) {
        var retailers = row.getRetailers().stream()
                .map(price -> new PersonalDataExport.RetailerPrice(price.retailerCode(), price.price(), price.previousPrice())).toList();
        return new PersonalDataExport.ProductPriceAlert(row.getId(), row.getProductCode(), row.getTarget(),
                row.getRetrievedAt(), row.getCreatedAt(), retailers, row.getReadAt(), row.getSnoozedUntil(),
                row.getRuleId(), row.getScheduledDueAt(), row.getPriceKind());
    }

    private PersonalDataExport.Preferences preferences() {
        var display = displayPreferences.current();
        var notifications = rows(NotificationSettingsEntity.class, "id").stream().findFirst()
                .map(settings -> new PersonalDataExport.Notifications(settings.getQuietStart(), settings.getQuietEnd(),
                        settings.getSummaryTime())).orElse(null);
        return new PersonalDataExport.Preferences(
                new PersonalDataExport.Display(display.language(), display.highContrast()), notifications);
    }

    private <T> List<T> rows(Class<T> type, String orderField) {
        return entities.createQuery("select entry from " + type.getSimpleName() + " entry order by entry." + orderField,
                type).getResultList();
    }

    private long count(Class<?> type) {
        return entities.createQuery("select count(entry) from " + type.getSimpleName() + " entry", Long.class)
                .getSingleResult();
    }

    private void delete(Class<?> type) {
        entities.createQuery("delete from " + type.getSimpleName()).executeUpdate();
    }

    public record Overview(long lists, long listEntries, long wishlist, long tracking,
                           long observations, long alerts, long reminders, long budgets) {}

    private record ListRow(Long listId, String productCode, LocalizedLabel name, Integer quantity,
                           Boolean checked, String retailerCode) {}
}
