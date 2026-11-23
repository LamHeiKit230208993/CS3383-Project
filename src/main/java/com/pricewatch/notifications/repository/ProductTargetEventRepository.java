package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.ProductTargetEventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface ProductTargetEventRepository extends JpaRepository<ProductTargetEventEntity, Long> {
    List<ProductTargetEventEntity> findTop100ByOrderByCreatedAtDescIdDesc();

    Optional<ProductTargetEventEntity> findByRuleIdAndScheduledDueAt(long ruleId, Instant scheduledDueAt);

    interface ReminderReference {
        Long getRuleId();
        String getProductCode();
    }

    @Query("""
            select event.ruleId as ruleId, event.productCode as productCode from ProductTargetEventEntity event
            where event.id = :id and event.cartQuantity is not null
            """)
    Optional<ReminderReference> findReminderReference(long id);

    @Query("""
            select event from ProductTargetEventEntity event, WatchRuleEntity rule
            where event.ruleId = rule.id and event.scheduledDueAt = rule.nextDueAt
                and event.cartQuantity is not null and event.readAt is null
                and (rule.paused is null or rule.paused = false)
            """)
    List<ProductTargetEventEntity> findActiveCartReminders();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from ProductTargetEventEntity event where event.id = :id")
    Optional<ProductTargetEventEntity> findLocked(long id);
}
