package com.pricewatch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "PRICEWATCH_CHAT_BASE_URL=http://127.0.0.1:9999/v1",
        "PRICEWATCH_CHAT_MODEL=fixture-model"
})
class ChatConfigurationTest {
    @Autowired
    private Environment environment;

    @Test
    void mapsChatEnvironmentOverridesToClientProperties() {
        assertEquals("http://127.0.0.1:9999/v1",
                environment.getProperty("pricewatch.chat.base-url"));
        assertEquals("fixture-model",
                environment.getProperty("pricewatch.chat.model"));
    }
}
