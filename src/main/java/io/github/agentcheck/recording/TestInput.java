package io.github.agentcheck.recording;

import java.util.List;

/**
 * One named input that should be executed while recording a golden-test draft.
 *
 * @param id stable case identifier
 * @param input text sent to the agent
 * @param description optional human-readable purpose
 * @param tags optional labels copied to the draft
 */
public record TestInput(
        String id,
        String input,
        String description,
        List<String> tags) {

    /** Validates required text and normalizes optional metadata. */
    public TestInput {
        requireNonBlank(id, "test input id");
        requireNonBlank(input, "test input text");
        description = description == null ? "" : description.strip();
        tags = normalizeTags(tags);
    }

    /**
     * Creates an input without description or tags.
     *
     * @param id stable case identifier
     * @param input text sent to the agent
     */
    public TestInput(String id, String input) {
        this(id, input, "", List.of());
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream().map(TestInput::normalizeTag).distinct().toList();
    }

    private static String normalizeTag(String tag) {
        requireNonBlank(tag, "test input tag");
        return tag.strip();
    }

    private static void requireNonBlank(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
    }
}
