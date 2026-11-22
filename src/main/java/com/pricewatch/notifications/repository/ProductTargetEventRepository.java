package com.pricewatch.notifications.repository;

import com.pricewatch.notifications.persistence.ProductTargetEventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductTargetEventRepository extends JpaRepository<ProductTargetEventEntity, Long> {
    List<ProductTargetEventEntity> findTop100ByOrderByCreatedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from ProductTargetEventEntity event where event.id = :id")
    Optional<ProductTargetEventEntity> findLocked(long id);
}
