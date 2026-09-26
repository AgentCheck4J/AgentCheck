package io.github.agentcheck.evaluation;

import java.util.List;

public record ToolResult(
        MetricValue accuracy,
        List<String> calledRequired,
        List<String> missingRequired,
        List<String> unexpectedCalled,
        List<String> forbiddenCalled) {

    public ToolResult {
        calledRequired = List.copyOf(calledRequired);
        missingRequired = List.copyOf(missingRequired);
        unexpectedCalled = List.copyOf(unexpectedCalled);
        forbiddenCalled = List.copyOf(forbiddenCalled);
    }
}
