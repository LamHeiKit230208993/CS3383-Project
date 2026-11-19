package com.pricewatch.notifications;

import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Objects;

/** Keeps historical notification preferences available in personal-data exports. */
@Service
public class NotificationPolicy {
    private static final LocalTime DEFAULT_QUIET_START = LocalTime.of(22, 0);
    private static final LocalTime DEFAULT_QUIET_END = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_SUMMARY_TIME = LocalTime.of(8, 0);

    private final NotificationSettingsRepository repository;

    public NotificationPolicy(NotificationSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Settings settings() {
        return repository.findById(1L).map(NotificationPolicy::toSettings)
                .orElseGet(() -> new Settings(DEFAULT_QUIET_START, DEFAULT_QUIET_END, DEFAULT_SUMMARY_TIME));
    }

    @Transactional
    public Settings update(LocalTime quietStart, LocalTime quietEnd, LocalTime summaryTime) {
        Objects.requireNonNull(quietStart, "quietStart");
        Objects.requireNonNull(quietEnd, "quietEnd");
        Objects.requireNonNull(summaryTime, "summaryTime");
        NotificationSettingsEntity entity = repository.findById(1L)
                .orElseGet(() -> new NotificationSettingsEntity(quietStart, quietEnd, summaryTime));
        entity.setTimes(quietStart, quietEnd, summaryTime);
        return toSettings(repository.save(entity));
    }

    private static Settings toSettings(NotificationSettingsEntity entity) {
        return new Settings(entity.getQuietStart(), entity.getQuietEnd(), entity.getSummaryTime());
    }

    public record Settings(LocalTime quietStart, LocalTime quietEnd, LocalTime summaryTime) {
    }
}
