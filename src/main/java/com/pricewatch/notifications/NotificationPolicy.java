package com.pricewatch.notifications;

import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import com.pricewatch.notifications.repository.NotificationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class NotificationPolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Hong_Kong");
    private static final LocalTime DEFAULT_QUIET_START = LocalTime.of(22, 0);
    private static final LocalTime DEFAULT_QUIET_END = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_SUMMARY_TIME = LocalTime.of(8, 0);

    private final NotificationSettingsRepository repository;

    public NotificationPolicy(NotificationSettingsRepository repository) {
        this.repository = repository;
    }

    public boolean isQuiet(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        Settings settings = settings();
        LocalTime current = instant.atZone(ZONE).toLocalTime();
        LocalTime start = settings.quietStart();
        LocalTime end = settings.quietEnd();
        if (start.equals(end)) return false;
        if (start.isBefore(end)) return !current.isBefore(start) && current.isBefore(end);
        return !current.isBefore(start) || current.isBefore(end);
    }

    public Instant nextAllowed(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        Settings settings = settings();
        if (!isQuiet(instant)) return instant;

        var local = instant.atZone(ZONE);
        LocalTime start = settings.quietStart();
        LocalTime end = settings.quietEnd();
        LocalDate endDate = start.isBefore(end) || local.toLocalTime().isBefore(end)
                ? local.toLocalDate()
                : local.toLocalDate().plusDays(1);
        return endDate.atTime(end).atZone(ZONE).toInstant();
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
