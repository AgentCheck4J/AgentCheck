package io.github.agentcheck.golden;

import java.util.List;

public record GoldenTestCase(
        String id,
        String input,
        ExpectedBehaviour expected,
        String description,
        List<String> tags,
        boolean enabled) {

    public GoldenTestCase {
        requireNonBlank(id, "case id");
        requireNonBlank(input, "case input");
        expected = expected == null ? ExpectedBehaviour.none() : expected;
        description = description == null ? "" : description.strip();
        tags = normalizeTags(tags);
    }

    public GoldenTestCase(String id, String input, ExpectedBehaviour expected) {
        this(id, input, expected, "", List.of(), true);
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream().map(GoldenTestCase::normalizeTag).distinct().toList();
    }

    private static String normalizeTag(String tag) {
        requireNonBlank(tag, "case tag");
        return tag.strip();
    }

    private static void requireNonBlank(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
    }
}
