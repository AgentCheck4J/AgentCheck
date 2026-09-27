package io.github.agentcheck.assertions;

import io.github.agentcheck.evaluation.EvaluationSuiteResult;

/** Lightweight assertion helpers for AgentCheck evaluation results. */
public final class AgentCheckAssertions {
    private final EvaluationSuiteResult actual;

    private AgentCheckAssertions(EvaluationSuiteResult actual) {
        if (actual == null) {
            throw new AssertionError("Expected an evaluation result but was null");
        }
        this.actual = actual;
    }

    /**
     * Starts an assertion for an evaluation result.
     *
     * @param actual result to inspect
     * @return assertions bound to the result
     * @throws AssertionError when the result is {@code null}
     */
    public static AgentCheckAssertions assertThat(EvaluationSuiteResult actual) {
        return new AgentCheckAssertions(actual);
    }

    /**
     * Requires the evaluation to contain no policy violations.
     *
     * @return this assertion object for further checks
     * @throws AssertionError when at least one policy violation exists
     */
    public AgentCheckAssertions hasNoPolicyViolations() {
        if (actual.policy().violations() != 0) {
            throw new AssertionError("Expected no policy violations but found " + actual.policy().violations());
        }
        return this;
    }
}
