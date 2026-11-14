package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.AlertEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEventEntity, Long> {
    boolean existsByTracking_IdAndObservationIdAndType(Long trackingId, Long observationId, String type);

    boolean existsBySummaryDateAndType(LocalDate summaryDate, String type);

    List<AlertEventEntity> findTop100ByOrderByCreatedAtDescIdDesc();

    List<AlertEventEntity> findByTypeInAndReadAtIsNullAndSummarizedAtIsNull(List<String> types);

    void deleteByTracking_IdAndTypeIn(Long trackingId, List<String> types);
}
