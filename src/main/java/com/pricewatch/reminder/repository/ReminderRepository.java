package com.pricewatch.reminder.repository;

import com.pricewatch.reminder.persistence.ReminderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    @EntityGraph(attributePaths = {"product", "product.listings"})
    List<ReminderEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"product", "product.listings"})
    Optional<ReminderEntity> findWithProductById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reminder from ReminderEntity reminder where reminder.id = :id")
    Optional<ReminderEntity> findLockedById(long id);

    @Query("""
            select reminder.id from ReminderEntity reminder
            where reminder.nextDueAt <= :now and (reminder.snoozedUntil is null or reminder.snoozedUntil <= :now)
            and (reminder.paused is null or reminder.paused = false)
            and not exists (
                select occurrence.id from ReminderOccurrenceEntity occurrence
                where occurrence.reminderId = reminder.id and occurrence.dueAt = reminder.nextDueAt
            )
            order by reminder.id
            """)
    List<Long> dueRuleIds(Instant now);
}
