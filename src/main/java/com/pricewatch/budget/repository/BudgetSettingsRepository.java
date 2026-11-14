package com.pricewatch.budget.repository;

import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetSettingsRepository extends JpaRepository<BudgetSettingsEntity, Long> {
}
