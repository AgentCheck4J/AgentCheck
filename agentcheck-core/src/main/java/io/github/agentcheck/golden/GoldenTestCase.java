package io.github.agentcheck.golden;

import java.util.List;

/**
 * One named input and its expected observable agent behaviour.
 *
 * @param id unique case identifier within its suite
 * @param input input sent to the agent
 * @param expected expected retrieval and tool behaviour
 * @param description optional human-readable description
 * @param tags tags used for deterministic suite selection
 * @param enabled whether the case should be executed
 */
public record GoldenTestCase(
        String id,
        String input,
        ExpectedBehaviour expected,
        String description,
        List<String> tags,
        boolean enabled) {

    /** Validates and normalizes the case definition. */
    public GoldenTestCase {
        requireNonBlank(id, "case id");
        requireNonBlank(input, "case input");
        expected = expected == null ? ExpectedBehaviour.none() : expected;
        description = description == null ? "" : description.strip();
        tags = normalizeTags(tags);
    }

    /**
     * Creates an enabled case without a description or tags.
     *
     * @param id unique case identifier
     * @param input input sent to the agent
     * @param expected expected behaviour
     */
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
