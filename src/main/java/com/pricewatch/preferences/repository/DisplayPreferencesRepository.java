package com.pricewatch.preferences.repository;

import com.pricewatch.preferences.persistence.DisplayPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisplayPreferencesRepository extends JpaRepository<DisplayPreferencesEntity, Long> {}
