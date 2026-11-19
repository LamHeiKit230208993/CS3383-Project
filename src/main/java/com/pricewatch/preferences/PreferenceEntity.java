package com.pricewatch.preferences;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class PreferenceEntity {
    @Id
    private Long id;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Column(name = "contrast", nullable = false, length = 16)
    private String contrast;

    protected PreferenceEntity() {
    }

    public PreferenceEntity(Long id, String locale, String contrast) {
        this.id = id;
        this.locale = locale;
        this.contrast = contrast;
    }

    public Long getId() {
        return id;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getContrast() {
        return contrast;
    }

    public void setContrast(String contrast) {
        this.contrast = contrast;
    }
}
