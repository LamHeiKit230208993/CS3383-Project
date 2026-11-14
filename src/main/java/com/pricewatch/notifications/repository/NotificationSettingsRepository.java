package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.NotificationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettingsEntity, Long> {
}
