package io.github.agentcheck.recording;

import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.model.ToolCall;

import java.util.List;
import java.util.Objects;

/**
 * A disabled case proposal containing inferred expectations and observations.
 * Its expectations must be reviewed before use as a golden test.
 *
 * @param id stable case identifier
 * @param input text that was sent to the agent
 * @param description optional human-readable purpose
 * @param tags optional labels copied to the generated case
 * @param proposedExpectations expectations inferred from observed behaviour
 * @param observedAnswer unverified answer returned by the agent
 * @param observedToolCalls unverified tool calls returned by the agent
 */
public record GoldenTestCaseDraft(
        String id,
        String input,
        String description,
        List<String> tags,
        ExpectedBehaviour proposedExpectations,
        String observedAnswer,
        List<ToolCall> observedToolCalls) {

    /** Validates required values and creates immutable metadata snapshots. */
    public GoldenTestCaseDraft {
        requireNonBlank(id, "draft case id");
        requireNonBlank(input, "draft case input");
        description = description == null ? "" : description.strip();
        tags = normalizeTags(tags);
        Objects.requireNonNull(proposedExpectations, "proposedExpectations");
        observedAnswer = observedAnswer == null ? "" : observedAnswer;
        observedToolCalls = observedToolCalls == null ? List.of() : List.copyOf(observedToolCalls);
    }

    private static void requireNonBlank(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream().map(GoldenTestCaseDraft::normalizeTag).distinct().toList();
    }

    private static String normalizeTag(String tag) {
        requireNonBlank(tag, "draft case tag");
        return tag.strip();
    }
}
