package com.pricewatch.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeepSeekAiClientTest {
    @Test
    void appendsChatCompletionsToAVersionedBaseUrl() {
        assertEquals("https://llm.cs.cityu.edu.hk/v1/chat/completions",
                DeepSeekAiClient.chatCompletions("https://llm.cs.cityu.edu.hk/v1").toString());
    }

    @Test
    void doesNotDoubleAppendThePathOrSlash() {
        assertEquals("https://host/v1/chat/completions",
                DeepSeekAiClient.chatCompletions("https://host/v1/").toString());
        assertEquals("https://host/v1/chat/completions",
                DeepSeekAiClient.chatCompletions("https://host/v1/chat/completions").toString());
    }
}
