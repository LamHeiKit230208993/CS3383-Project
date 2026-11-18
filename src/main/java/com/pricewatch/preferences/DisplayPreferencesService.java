package com.pricewatch.preferences;

import com.pricewatch.preferences.persistence.DisplayPreferencesEntity;
import com.pricewatch.preferences.repository.DisplayPreferencesRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DisplayPreferencesService {
    private static final long LOCAL_SHOPPER = 1L;
    private static final Set<String> LANGUAGES = Set.of("en", "zh-Hant", "zh-Hans");
    private final DisplayPreferencesRepository repository;

    public DisplayPreferencesService(DisplayPreferencesRepository repository) {
        this.repository = repository;
    }

    public View current() {
        return repository.findById(LOCAL_SHOPPER)
                .map(saved -> new View(saved.getLanguage(), saved.isHighContrast()))
                .orElseGet(() -> new View("en", false));
    }

    public synchronized void update(String language, boolean highContrast) {
        if (language == null || !LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("Unsupported display language");
        }
        repository.save(new DisplayPreferencesEntity(LOCAL_SHOPPER, language, highContrast));
    }

    public void reset() {
        repository.deleteById(LOCAL_SHOPPER);
    }

    public record View(String language, boolean highContrast) {}
}
