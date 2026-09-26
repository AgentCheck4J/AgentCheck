package io.github.agentcheck.evaluation;

import java.util.List;

public record RetrievalResult(int k, MetricValue recallAtK, MetricValue reciprocalRank, List<String> retrievedRelevantDocuments) {
    public RetrievalResult {
        retrievedRelevantDocuments = List.copyOf(retrievedRelevantDocuments);
    }
}
