package com.pricewatch.preferences.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "display_preferences")
public class DisplayPreferencesEntity {
    @Id
    private Long id;

    @Column(name = "language_tag", nullable = false, length = 12)
    private String language;

    @Column(nullable = false)
    private boolean highContrast;

    protected DisplayPreferencesEntity() {}

    public DisplayPreferencesEntity(long id, String language, boolean highContrast) {
        this.id = id;
        this.language = language;
        this.highContrast = highContrast;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isHighContrast() {
        return highContrast;
    }
}
