package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.ProductTargetRuleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductTargetRuleRepository extends JpaRepository<ProductTargetRuleEntity, String> {
    @Query("select rule.productCode from ProductTargetRuleEntity rule")
    List<String> findProductCodes();

    List<ProductTargetRuleEntity> findAllByOrderByProductCode();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rule from ProductTargetRuleEntity rule
            where rule.paused = false and exists (
                select 1 from SavedProductEntity saved where saved.productCode = rule.productCode
            )
            order by rule.productCode
            """)
    List<ProductTargetRuleEntity> findWishlistedRulesForUpdate();
}
