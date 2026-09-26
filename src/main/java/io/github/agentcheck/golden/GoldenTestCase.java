package io.github.agentcheck.golden;

public record GoldenTestCase(String id, String input, ExpectedBehaviour expected) {
    public GoldenTestCase {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("case id must not be blank");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("case input must not be blank");
        expected = expected == null ? ExpectedBehaviour.none() : expected;
    }
}
