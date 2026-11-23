package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.WatchRuleEntity;
import com.pricewatch.notifications.WatchPriceKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WatchRuleRepository extends JpaRepository<WatchRuleEntity, Long> {
    List<WatchRuleEntity> findByProductCodeOrderById(String productCode);

    List<WatchRuleEntity> findAllByOrderById();

    @Query("""
            select distinct rule.productCode from WatchRuleEntity rule
            where rule.target is not null and rule.priceKind in :kinds
                and rule.completedAt is null and (rule.paused is null or rule.paused = false)
            """)
    List<String> priceBaselineProductCodes(List<WatchPriceKind> kinds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rule from WatchRuleEntity rule where rule.id = :id and rule.productCode = :code")
    Optional<WatchRuleEntity> findForUpdate(long id, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rule from WatchRuleEntity rule
            where rule.target is not null and rule.completedAt is null
                and (rule.paused is null or rule.paused = false)
                and exists (select 1 from SavedProductEntity saved where saved.productCode = rule.productCode)
            order by rule.id
            """)
    List<WatchRuleEntity> findWishlistedForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rule from WatchRuleEntity rule
            where rule.nextDueAt <= :now and rule.waitingForLive = false
                and rule.completedAt is null and (rule.paused is null or rule.paused = false)
                and (rule.target is null or exists (
                    select 1 from SavedProductEntity saved where saved.productCode = rule.productCode
                ))
            order by rule.id
            """)
    List<WatchRuleEntity> findNewlyDueForUpdate(Instant now);
}
