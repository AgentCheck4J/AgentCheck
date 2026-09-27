package io.github.agentcheck.assertions;

import io.github.agentcheck.evaluation.EvaluationSuiteResult;

public final class AgentCheckAssertions {
    private final EvaluationSuiteResult actual;

    private AgentCheckAssertions(EvaluationSuiteResult actual) {
        if (actual == null) {
            throw new AssertionError("Expected an evaluation result but was null");
        }
        this.actual = actual;
    }

    public static AgentCheckAssertions assertThat(EvaluationSuiteResult actual) {
        return new AgentCheckAssertions(actual);
    }

    public AgentCheckAssertions hasNoPolicyViolations() {
        if (actual.policy().violations() != 0) {
            throw new AssertionError("Expected no policy violations but found " + actual.policy().violations());
        }
        return this;
    }
}
