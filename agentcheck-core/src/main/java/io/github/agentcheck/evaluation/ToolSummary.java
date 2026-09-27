package io.github.agentcheck.evaluation;

/**
 * Aggregated tool-behaviour result for a suite.
 *
 * @param accuracy mean tool accuracy
 * @param missing number of missing required tools
 * @param unexpected number of unexpected tool calls
 */
public record ToolSummary(MetricValue accuracy, int missing, int unexpected) { }
