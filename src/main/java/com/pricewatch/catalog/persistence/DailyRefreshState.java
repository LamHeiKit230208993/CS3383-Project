package com.pricewatch.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "daily_refresh_state")
public class DailyRefreshState {
    public enum Outcome { STARTED, ACCEPTED, FAILED }

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private LocalDate refreshDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Outcome outcome;

    protected DailyRefreshState() {}

    public DailyRefreshState(LocalDate refreshDay, Outcome outcome) {
        this.refreshDay = refreshDay;
        this.outcome = outcome;
    }

    public LocalDate refreshDay() { return refreshDay; }

    public Outcome outcome() { return outcome; }
}
