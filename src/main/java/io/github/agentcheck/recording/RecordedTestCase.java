package io.github.agentcheck.recording;

import io.github.agentcheck.model.AgentExecution;

import java.util.Objects;

/**
 * Associates input metadata with the agent execution observed for that input.
 *
 * @param testInput metadata for the recorded case
 * @param execution observed agent execution with matching input text
 */
public record RecordedTestCase(TestInput testInput, AgentExecution execution) {
    /** Validates that metadata and execution describe the same input. */
    public RecordedTestCase {
        Objects.requireNonNull(testInput, "testInput");
        Objects.requireNonNull(execution, "execution");
        if (!testInput.input().equals(execution.input())) {
            throw new IllegalArgumentException(
                    "recorded execution input does not match test input '" + testInput.id() + "'");
        }
    }
}
