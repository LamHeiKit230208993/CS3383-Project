package com.pricewatch.ask;

import java.util.Map;

/** A model may explain checked facts, but it never supplies or changes them. */
public interface AiAnswerClient {
    String answer(Map<String, Object> checkedFacts, String question);
}
