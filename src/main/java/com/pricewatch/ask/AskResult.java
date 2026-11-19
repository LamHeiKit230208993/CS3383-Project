package com.pricewatch.ask;

import java.util.List;

public record AskResult(
        Status status,
        String question,
        AskFacts facts,
        List<Candidate> candidates,
        String answer,
        boolean fallback) {

    public AskResult {
        candidates = List.copyOf(candidates);
    }

    public enum Status {
        ANSWERED, CHOOSE, NO_MATCH
    }

    public record Candidate(String code, String name, String brand, String category) {
    }

    public static AskResult noMatch(String question) {
        return new AskResult(Status.NO_MATCH, question, null, List.of(), null, false);
    }

    public static AskResult choose(String question, List<Candidate> candidates) {
        return new AskResult(Status.CHOOSE, question, null, candidates, null, false);
    }

    public static AskResult answered(String question, AskFacts facts, String answer, boolean fallback) {
        return new AskResult(Status.ANSWERED, question, facts, List.of(), answer, fallback);
    }
}
