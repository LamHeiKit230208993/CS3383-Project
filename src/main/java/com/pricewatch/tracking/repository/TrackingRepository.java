package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.TrackingEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackingRepository extends JpaRepository<TrackingEntity, Long> {
    @Query("select tracking.product.code from TrackingEntity tracking where tracking.id = :id")
    Optional<String> findProductCode(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tracking from TrackingEntity tracking where tracking.product.code = :code and tracking.enabled = true order by tracking.startedAt desc, tracking.id desc")
    List<TrackingEntity> findEnabledForProductForUpdate(@Param("code") String code);

    Optional<TrackingEntity> findByProduct_CodeAndRetailerCodeAndEnabledTrue(String productCode, String retailerCode);

    @EntityGraph(attributePaths = "product")
    List<TrackingEntity> findAllByOrderByStartedAtDescIdDesc();

    @EntityGraph(attributePaths = "product")
    List<TrackingEntity> findAllByEnabledTrueOrderByStartedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tracking from TrackingEntity tracking where tracking.id = :id")
    Optional<TrackingEntity> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "product")
    @Query("select tracking from TrackingEntity tracking where tracking.enabled = true order by tracking.startedAt desc, tracking.id desc")
    List<TrackingEntity> findAllEnabledForUpdate();
}
