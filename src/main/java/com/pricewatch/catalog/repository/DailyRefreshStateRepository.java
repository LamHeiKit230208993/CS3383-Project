package com.pricewatch.catalog.repository;

import com.pricewatch.catalog.persistence.DailyRefreshState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRefreshStateRepository extends JpaRepository<DailyRefreshState, Long> {
}
