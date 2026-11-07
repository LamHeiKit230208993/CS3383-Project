package com.pricewatch.alerts.repository;

import com.pricewatch.alerts.persistence.AlertEventEntity;
import com.pricewatch.alerts.persistence.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEventEntity, Long> {
    List<AlertEventEntity> findAllByOrderByCreatedAtDescIdDesc();
    List<AlertEventEntity> findAllByVisibleFalseOrderByCreatedAtAscIdAsc();
    boolean existsByProductCodeAndRetailerCodeAndTypeAndPrice(
            String productCode, String retailerCode, AlertRuleEntity.Type type, BigDecimal price);
}
