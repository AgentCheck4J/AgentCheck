package io.github.agentcheck.evaluation;

/**
 * Aggregated policy result for a suite.
 *
 * @param violations number of policy violations
 */
public record PolicySummary(int violations) { }
