package com.pricewatch.reminder.repository;

import com.pricewatch.reminder.persistence.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    Optional<ReminderEntity> findByProductCode(String productCode);
    List<ReminderEntity> findAllByOrderByDueAtAscIdAsc();
    List<ReminderEntity> findAllByDueAtLessThanEqualOrderByDueAtAscIdAsc(Instant now);
}
