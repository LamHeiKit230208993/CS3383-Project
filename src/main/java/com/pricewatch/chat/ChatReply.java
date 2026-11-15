package com.pricewatch.chat;

/**
 * One chat turn. {@code kind} is {@code answer} or {@code clarify}; {@code checkedFacts}
 * holds the catalogue facts the answer was derived from (empty when clarifying).
 */
public record ChatReply(String kind, String message, String checkedFacts, boolean answeredByModel) {
    public static ChatReply answer(String message, String checkedFacts, boolean answeredByModel) {
        return new ChatReply("answer", message, checkedFacts, answeredByModel);
    }

    public static ChatReply clarify(String message) {
        return new ChatReply("clarify", message, "", false);
    }

    public boolean isClarify() {
        return "clarify".equals(kind);
    }
}
