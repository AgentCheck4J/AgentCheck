package io.github.agentcheck.evaluation;

import java.util.List;

/**
 * Tool-behaviour result for one evaluated case.
 *
 * @param accuracy deterministic tool accuracy
 * @param calledRequired required tools that were called
 * @param missingRequired required tools that were not called
 * @param unexpectedCalled called tools without an expectation
 * @param forbiddenCalled forbidden tools that were called
 */
public record ToolResult(
        MetricValue accuracy,
        List<String> calledRequired,
        List<String> missingRequired,
        List<String> unexpectedCalled,
        List<String> forbiddenCalled) {

    /** Creates an immutable tool result. */
    public ToolResult {
        calledRequired = List.copyOf(calledRequired);
        missingRequired = List.copyOf(missingRequired);
        unexpectedCalled = List.copyOf(unexpectedCalled);
        forbiddenCalled = List.copyOf(forbiddenCalled);
    }
}
