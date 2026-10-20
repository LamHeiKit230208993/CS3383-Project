package com.pricewatch.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Sample REST endpoints under {@code /api/**}.
 *
 * <p>The Vite dev server (port 5173) proxies {@code /api/*} to Spring Boot
 * (port 8080), so the React app can call same-origin paths in both dev and
 * production without extra configuration.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "pricewatch-backend",
            "time", Instant.now().toString()
        );
    }
}
