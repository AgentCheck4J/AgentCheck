package io.github.agentcheck.evaluation;

public record ToolSummary(MetricValue accuracy, int missing, int unexpected) { }
