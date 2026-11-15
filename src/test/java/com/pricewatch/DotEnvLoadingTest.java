package com.pricewatch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.main.web-application-type=none")
class DotEnvLoadingTest {
    @Value("${pricewatch.ai.api-key:}")
    String apiKey;

    @Test
    void dotEnvFileFeedsThePlaceholder() throws Exception {
        Path env = Files.createTempFile("dotenv-test", ".env");
        Files.writeString(env, """
                # comment
                PRICEWATCH_AI_BASE_URL=https://example.test/v1
                PRICEWATCH_AI_API_KEY="secret-key"
                """);
        PricewatchApplication.loadDotEnv(env);

        assertEquals("https://example.test/v1", System.getProperty("PRICEWATCH_AI_BASE_URL"));
        assertEquals("secret-key", System.getProperty("PRICEWATCH_AI_API_KEY"));
        Files.deleteIfExists(env);
    }

    @Test
    void missingFileIsIgnored() {
        PricewatchApplication.loadDotEnv(Path.of("does-not-exist.env"));
        assertTrue(true);
    }
}
