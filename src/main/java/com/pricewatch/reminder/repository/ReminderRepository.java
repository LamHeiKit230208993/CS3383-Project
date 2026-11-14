package com.pricewatch.reminder.repository;

import com.pricewatch.reminder.persistence.ReminderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    @EntityGraph(attributePaths = {"product", "product.listings"})
    List<ReminderEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"product", "product.listings"})
    Optional<ReminderEntity> findWithProductById(Long id);
}
