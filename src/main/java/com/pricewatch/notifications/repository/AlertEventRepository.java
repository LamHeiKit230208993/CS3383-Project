package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.AlertEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEventEntity, Long> {
    boolean existsByTracking_IdAndObservationIdAndType(Long trackingId, Long observationId, String type);

    List<AlertEventEntity> findTop100ByOrderByCreatedAtDescIdDesc();

    void deleteByTracking_IdAndTypeIn(Long trackingId, List<String> types);
}
