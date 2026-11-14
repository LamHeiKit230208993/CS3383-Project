package com.pricewatch.alerts.repository;

import com.pricewatch.alerts.persistence.AlertEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEventEntity, Long> {
    List<AlertEventEntity> findAllByOrderByFiredAtDescIdDesc();
    List<AlertEventEntity> findAllByFiredAtAfterOrderByFiredAtDescIdDesc(Instant after);
    boolean existsByRuleIdAndObservationId(Long ruleId, Long observationId);
}
