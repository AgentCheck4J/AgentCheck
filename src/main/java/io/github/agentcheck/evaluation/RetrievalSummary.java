package io.github.agentcheck.evaluation;

public record RetrievalSummary(int k, MetricValue recallAtK, MetricValue mrr) { }
