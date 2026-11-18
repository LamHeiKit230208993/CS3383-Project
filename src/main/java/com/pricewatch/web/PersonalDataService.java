package com.pricewatch.web;

import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * J12 R45: view, export and delete the one local user's stored data.
 * The accepted catalogue and its import metadata are source data, not the
 * user's records, so delete keeps them.
 */
@Service
public class PersonalDataService {
    private final ShoppingService shopping;
    private final TrackingService tracking;
    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository listItemRepository;
    private final SavedProductRepository savedRepository;
    private final TrackingRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final AlertEventRepository alertEventRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final ReminderRepository reminderRepository;
    private final BudgetSettingsRepository budgetSettingsRepository;
    private final ObjectMapper mapper;

    public PersonalDataService(ShoppingService shopping, TrackingService tracking,
                               ShoppingListRepository listRepository,
                               ShoppingListItemRepository listItemRepository,
                               SavedProductRepository savedRepository,
                               TrackingRepository trackingRepository,
                               PriceObservationRepository observationRepository,
                               AlertEventRepository alertEventRepository,
                               NotificationSettingsRepository notificationSettingsRepository,
                               ReminderRepository reminderRepository,
                               BudgetSettingsRepository budgetSettingsRepository,
                               ObjectMapper mapper) {
        this.shopping = shopping;
        this.tracking = tracking;
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.savedRepository = savedRepository;
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.alertEventRepository = alertEventRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.reminderRepository = reminderRepository;
        this.budgetSettingsRepository = budgetSettingsRepository;
        this.mapper = mapper;
    }

    /** Counts shown on /my-data. Only local user records, never source data. */
    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(
                shopping.items().size(),
                savedRepository.count(),
                trackingRepository.count(),
                observationRepository.count(),
                alertEventRepository.count(),
                notificationSettingsRepository.count() > 0,
                reminderRepository.count(),
                budgetSettingsRepository.count() > 0);
    }

    public record Overview(long listItems, long savedProducts, long trackingPeriods, long priceObservations,
                           long alertEvents, boolean alertSettings, long reminders, boolean budgetSettings) {
    }

    /** Every local user record as one JSON document. Amounts keep their exact decimals. */
    @Transactional(readOnly = true)
    public byte[] export() {
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Market Desk");
        root.put("kind", "local-user-data");
        root.put("exportedAt", Instant.now().toString());
        root.put("note", "One local user. The accepted catalogue is source data and is not included.");

        ArrayNode items = root.putArray("listItems");
        for (var item : shopping.items()) {
            ObjectNode node = items.addObject();
            node.put("code", item.product().code());
            node.put("name", item.product().name().en());
            node.put("quantity", item.quantity());
            node.put("checked", item.checked());
            putAmount(node, "unitPrice", item.unitPrice());
            node.put("retailerCode", item.retailerCode());
            node.put("selectedRetailerCode", item.selectedRetailerCode());
        }

        ArrayNode saved = root.putArray("wishlist");
        tracking.savedProducts().forEach(entry -> saved.add(entry.product().code()));

        ArrayNode periods = root.putArray("tracking");
        for (var period : tracking.trackingPeriods()) {
            ObjectNode node = periods.addObject();
            node.put("id", period.id());
            node.put("productCode", period.productCode());
            node.put("productName", period.productName());
            node.put("retailerCode", period.retailerCode());
            node.put("enabled", period.enabled());
            putTime(node, "startedAt", period.startedAt());
            putTime(node, "stoppedAt", period.stoppedAt());
            putAmount(node, "targetPrice", period.targetPrice());
            node.put("dropEnabled", period.dropEnabled());
            ArrayNode observations = node.putArray("observations");
            for (var observation : period.observations()) {
                ObjectNode entry = observations.addObject();
                putAmount(entry, "price", observation.price());
                putTime(entry, "observedAt", observation.observedAt());
                entry.put("sourceUrl", observation.sourceUrl());
            }
        }
        return toBytes(root);
    }

    /** Removes only this user's records: list, wishlist, tracking, history, alerts, reminders, budget. */
    @Transactional
    public void deleteLocalData() {
        listItemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        alertEventRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        savedRepository.deleteAllInBatch();
        reminderRepository.deleteAllInBatch();
        notificationSettingsRepository.deleteAllInBatch();
        budgetSettingsRepository.deleteAllInBatch();
    }

    private void putAmount(ObjectNode node, String field, BigDecimal value) {
        if (value == null) node.putNull(field);
        else node.put(field, value.toPlainString());
    }

    private void putTime(ObjectNode node, String field, Instant value) {
        if (value == null) node.putNull(field);
        else node.put(field, value.toString());
    }

    private byte[] toBytes(ObjectNode root) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Could not build the data export.", exception);
        }
    }
}
