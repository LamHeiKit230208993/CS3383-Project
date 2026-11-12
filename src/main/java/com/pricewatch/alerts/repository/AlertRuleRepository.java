package com.pricewatch.alerts.repository;

import com.pricewatch.alerts.persistence.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {
    List<AlertRuleEntity> findAllByEnabledTrue();
    List<AlertRuleEntity> findAllByOrderByIdAsc();
}
