package com.pricewatch.alerts.repository;

import com.pricewatch.alerts.persistence.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {
    List<AlertRuleEntity> findAllByEnabledTrue();
    List<AlertRuleEntity> findAllByProductCodeAndEnabledTrue(String productCode);
    Optional<AlertRuleEntity> findFirstByProductCodeAndTypeAndEnabledTrue(
            String productCode, AlertRuleEntity.Type type);
}
