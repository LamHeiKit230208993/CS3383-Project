package com.pricewatch.preferences;

import com.pricewatch.i18n.AppMessages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class PreferenceService {
    public static final String ENGLISH = "en";
    public static final String TRADITIONAL_CHINESE = "zh-HK";
    public static final String SIMPLIFIED_CHINESE = "zh-CN";
    public static final String NORMAL_CONTRAST = "normal";
    public static final String HIGH_CONTRAST = "high";

    private static final long SETTINGS_ID = 1L;

    private final PreferenceRepository repository;

    public PreferenceService(PreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Settings settings() {
        return repository.findById(SETTINGS_ID)
                .map(entity -> new Settings(entity.getLocale(), entity.getContrast()))
                .orElseGet(Settings::defaults);
    }

    @Transactional(readOnly = true)
    public Locale locale() {
        return toLocale(settings().locale());
    }

    @Transactional
    public Settings update(String locale, String contrast) {
        String normalizedLocale = normalizeLocale(locale);
        String normalizedContrast = normalizeContrast(contrast);
        PreferenceEntity entity = repository.findById(SETTINGS_ID)
                .orElseGet(() -> new PreferenceEntity(SETTINGS_ID, normalizedLocale, normalizedContrast));
        entity.setLocale(normalizedLocale);
        entity.setContrast(normalizedContrast);
        repository.save(entity);
        return new Settings(normalizedLocale, normalizedContrast);
    }

    public static String normalizeLocale(String value) {
        if (value == null) throw new IllegalArgumentException(AppMessages.text("preferences.invalidLanguage"));
        return switch (value.trim()) {
            case ENGLISH -> ENGLISH;
            case TRADITIONAL_CHINESE, "zh-Hant", "zh-TW" -> TRADITIONAL_CHINESE;
            case SIMPLIFIED_CHINESE, "zh-Hans", "zh" -> SIMPLIFIED_CHINESE;
            default -> throw new IllegalArgumentException(AppMessages.text("preferences.invalidLanguage"));
        };
    }

    public static String normalizeContrast(String value) {
        if (value == null) throw new IllegalArgumentException(AppMessages.text("preferences.invalidContrast"));
        return switch (value.trim()) {
            case NORMAL_CONTRAST, "standard" -> NORMAL_CONTRAST;
            case HIGH_CONTRAST -> HIGH_CONTRAST;
            default -> throw new IllegalArgumentException(AppMessages.text("preferences.invalidContrast"));
        };
    }

    public static Locale toLocale(String locale) {
        return switch (normalizeLocale(locale)) {
            case TRADITIONAL_CHINESE -> Locale.forLanguageTag(TRADITIONAL_CHINESE);
            case SIMPLIFIED_CHINESE -> Locale.forLanguageTag(SIMPLIFIED_CHINESE);
            default -> Locale.ENGLISH;
        };
    }

    public static String htmlLang(String locale) {
        return switch (normalizeLocale(locale)) {
            case TRADITIONAL_CHINESE -> "zh-Hant";
            case SIMPLIFIED_CHINESE -> "zh-Hans";
            default -> "en";
        };
    }

    public record Settings(String locale, String contrast) {
        public Settings {
            locale = normalizeLocale(locale);
            contrast = normalizeContrast(contrast);
        }

        public static Settings defaults() {
            return new Settings(ENGLISH, NORMAL_CONTRAST);
        }

        public Locale javaLocale() {
            return toLocale(locale);
        }

        public String htmlLang() {
            return PreferenceService.htmlLang(locale);
        }

        public boolean highContrast() {
            return HIGH_CONTRAST.equals(contrast);
        }
    }
}
