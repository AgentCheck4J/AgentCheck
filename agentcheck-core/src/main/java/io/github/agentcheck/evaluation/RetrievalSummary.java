package io.github.agentcheck.evaluation;

/**
 * Aggregated retrieval metrics for a suite.
 *
 * @param k retrieval cutoff
 * @param recallAtK mean recall at the configured cutoff
 * @param mrr mean reciprocal rank
 */
public record RetrievalSummary(int k, MetricValue recallAtK, MetricValue mrr) { }
