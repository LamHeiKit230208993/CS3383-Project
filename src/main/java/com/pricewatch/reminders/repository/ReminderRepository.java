package com.pricewatch.reminders.repository;

import com.pricewatch.reminders.persistence.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    List<ReminderEntity> findAllByOrderByIdAsc();
}
