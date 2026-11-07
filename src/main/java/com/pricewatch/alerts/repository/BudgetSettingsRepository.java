package com.pricewatch.alerts.repository;

import com.pricewatch.alerts.persistence.BudgetSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetSettingsRepository extends JpaRepository<BudgetSettingsEntity, Long> {
}
