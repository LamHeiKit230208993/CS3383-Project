package com.pricewatch.reminder.repository;

import com.pricewatch.reminder.persistence.ReminderOccurrenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderOccurrenceRepository extends JpaRepository<ReminderOccurrenceEntity, Long> {
    Optional<ReminderOccurrenceEntity> findByReminderIdAndDueAt(long reminderId, Instant dueAt);

    List<ReminderOccurrenceEntity> findAllByOrderByCreatedAtDescIdDesc();
}
