package io.github.agentcheck.evaluation;

import java.util.List;

/**
 * Retrieval metrics for one evaluated case.
 *
 * @param k retrieval cutoff
 * @param recallAtK recall at the configured cutoff
 * @param reciprocalRank reciprocal rank of the first relevant document
 * @param retrievedRelevantDocuments relevant document IDs found in the normalized ranking
 */
public record RetrievalResult(
        int k,
        MetricValue recallAtK,
        MetricValue reciprocalRank,
        List<String> retrievedRelevantDocuments) {
    /** Creates an immutable retrieval result. */
    public RetrievalResult {
        retrievedRelevantDocuments = List.copyOf(retrievedRelevantDocuments);
    }
}
