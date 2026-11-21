package com.pricewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class PricewatchApplication {

    public static void main(String[] args) {
        loadDotEnv(Path.of(".env"));
        SpringApplication.run(PricewatchApplication.class, args);
    }

    /**
     * Loads KEY=VALUE lines from a local .env into the environment (never committed),
     * so the AI settings do not have to be exported in the shell. Real environment
     * variables already set take precedence over the file.
     */
    static void loadDotEnv(Path file) {
        if (!Files.isRegularFile(file)) return;
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException exception) {
            return;
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();
            if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        }
    }
}
