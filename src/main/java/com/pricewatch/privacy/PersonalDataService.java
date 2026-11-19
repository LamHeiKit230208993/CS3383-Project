package com.pricewatch.privacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.budget.repository.BudgetSettingsRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.persistence.AlertEventEntity;
import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import com.pricewatch.preferences.PreferenceEntity;
import com.pricewatch.preferences.PreferenceRepository;
import com.pricewatch.reminder.persistence.ReminderEntity;
import com.pricewatch.reminder.repository.ReminderRepository;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** View, export, and delete the one local user's records without touching the accepted catalogue. */
@Service
public class PersonalDataService {
    private static final String FORMAT = "pricewatch-personal-data";
    private static final int VERSION = 1;
    private static final long SETTINGS_ID = 1L;

    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository itemRepository;
    private final SavedProductRepository savedRepository;
    private final TrackingRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final AlertEventRepository alertEventRepository;
    private final ReminderRepository reminderRepository;
    private final BudgetSettingsRepository budgetRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final PreferenceRepository preferenceRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PersonalDataService(ShoppingListRepository listRepository,
                               ShoppingListItemRepository itemRepository,
                               SavedProductRepository savedRepository,
                               TrackingRepository trackingRepository,
                               PriceObservationRepository observationRepository,
                               AlertEventRepository alertEventRepository,
                               ReminderRepository reminderRepository,
                               BudgetSettingsRepository budgetRepository,
                               NotificationSettingsRepository notificationSettingsRepository,
                               PreferenceRepository preferenceRepository,
                               ProductRepository productRepository,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.savedRepository = savedRepository;
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.alertEventRepository = alertEventRepository;
        this.reminderRepository = reminderRepository;
        this.budgetRepository = budgetRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.preferenceRepository = preferenceRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PersonalDataSummary summary() {
        return new PersonalDataSummary(
                listRepository.count(),
                itemRepository.count(),
                savedRepository.count(),
                trackingRepository.count(),
                observationRepository.count(),
                alertEventRepository.count(),
                reminderRepository.count(),
                preferenceRepository.count() > 0,
                budgetRepository.count() > 0,
                notificationSettingsRepository.count() > 0);
    }

    @Transactional(readOnly = true)
    public byte[] export() {
        PersonalDataExport export = new PersonalDataExport(FORMAT, VERSION, clock.instant(),
                lists(), wishlist(), tracking(), alerts(), budget(), reminders(),
                preferences(), notificationSettings());
        try {
            return objectMapper.writeValueAsBytes(export);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write the personal-data export.", exception);
        }
    }

    private List<PersonalDataExport.ListRecord> lists() {
        return listRepository.findAllByOrderByIdAsc().stream()
                .map(this::list)
                .toList();
    }

    private PersonalDataExport.ListRecord list(ShoppingListEntity list) {
        List<PersonalDataExport.ItemRecord> items = itemRepository
                .findAllByShoppingList_IdOrderByIdAsc(list.getId()).stream()
                .map(this::item)
                .toList();
        return new PersonalDataExport.ListRecord(list.getName(), items);
    }

    private PersonalDataExport.ItemRecord item(ShoppingListItemEntity item) {
        return new PersonalDataExport.ItemRecord(item.getProductCode(), item.getQuantity(),
                item.isChecked(), item.getSelectedRetailerCode());
    }

    private List<PersonalDataExport.WishlistRecord> wishlist() {
        return savedRepository.findAllByOrderBySavedAtDesc().stream()
                .map(entry -> new PersonalDataExport.WishlistRecord(entry.getProductCode(), entry.getSavedAt()))
                .toList();
    }

    private List<PersonalDataExport.TrackingRecord> tracking() {
        return trackingRepository.findAllByOrderByStartedAtDescIdDesc().stream()
                .map(this::tracking)
                .toList();
    }

    private PersonalDataExport.TrackingRecord tracking(TrackingEntity period) {
        List<PersonalDataExport.ObservationRecord> observations = observationRepository
                .findByTracking_IdOrderByObservedAtAscIdAsc(period.getId()).stream()
                .map(this::observation)
                .toList();
        return new PersonalDataExport.TrackingRecord(period.getId(), period.getProductCode(),
                period.getRetailerCode(), period.isEnabled(), period.getStartedAt(), period.getStoppedAt(),
                period.getTargetPrice(), period.isDropEnabled(), observations);
    }

    private PersonalDataExport.ObservationRecord observation(PriceObservationEntity observation) {
        return new PersonalDataExport.ObservationRecord(observation.getPrice(), observation.getObservedAt());
    }

    private List<PersonalDataExport.AlertRecord> alerts() {
        return alertEventRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(this::alert)
                .toList();
    }

    private PersonalDataExport.AlertRecord alert(AlertEventEntity event) {
        return new PersonalDataExport.AlertRecord(event.getId(),
                event.getTracking() == null ? null : event.getTracking().getId(),
                event.getType(), event.getPrice(), event.getObservedAt(), event.getPreviousPrice(),
                event.getPreviousObservedAt(), event.getCreatedAt(), event.getReadAt(), event.getSnoozedUntil(),
                event.getSummaryDate(), event.getSummaryCount());
    }

    private PersonalDataExport.BudgetRecord budget() {
        return budgetRepository.findById(SETTINGS_ID)
                .map(settings -> new PersonalDataExport.BudgetRecord(settings.getPeriod().name(),
                        settings.getLimitAmount()))
                .orElse(null);
    }

    private List<PersonalDataExport.ReminderRecord> reminders() {
        return reminderRepository.findAllByOrderByIdAsc().stream()
                .map(this::reminder)
                .toList();
    }

    private PersonalDataExport.ReminderRecord reminder(ReminderEntity reminder) {
        return new PersonalDataExport.ReminderRecord(reminder.getId(), reminder.getProduct().getCode(),
                reminder.getQuantity(), reminder.getIntervalDays(), reminder.getSelectedRetailerCode(),
                reminder.getNextDueAt(), reminder.getSnoozedUntil());
    }

    private PersonalDataExport.PreferencesRecord preferences() {
        return preferenceRepository.findById(SETTINGS_ID)
                .map(this::preferences)
                .orElse(null);
    }

    private PersonalDataExport.PreferencesRecord preferences(PreferenceEntity entity) {
        return new PersonalDataExport.PreferencesRecord(entity.getLocale(), entity.getContrast());
    }

    private PersonalDataExport.NotificationSettingsRecord notificationSettings() {
        return notificationSettingsRepository.findById(SETTINGS_ID)
                .map(this::notificationSettings)
                .orElse(null);
    }

    private PersonalDataExport.NotificationSettingsRecord notificationSettings(NotificationSettingsEntity entity) {
        return new PersonalDataExport.NotificationSettingsRecord(entity.getQuietStart(), entity.getQuietEnd(),
                entity.getSummaryTime());
    }

    /**
     * Deletes personal records only. The accepted catalogue and its accepted-import
     * metadata remain, while product-wide tracking choices are reset.
     */
    @Transactional
    public void deleteAll() {
        alertEventRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        savedRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        reminderRepository.deleteAllInBatch();
        budgetRepository.deleteAllInBatch();
        notificationSettingsRepository.deleteAllInBatch();
        preferenceRepository.deleteAllInBatch();
        productRepository.clearTrackingChoices();
    }
}
