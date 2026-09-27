package io.github.agentcheck.model;

/**
 * Document observed in an agent's ranked retrieval result.
 *
 * @param id stable document identifier
 * @param rank one-based rank in the result
 * @param score optional provider-specific similarity score
 */
public record RetrievedDocument(String id, int rank, Double score) {
    /** Validates the document identifier and rank. */
    public RetrievedDocument {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("document id must not be blank");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("document rank must be at least 1");
        }
    }

    /**
     * Creates a retrieved document without a similarity score.
     *
     * @param id stable document identifier
     * @param rank one-based rank
     */
    public RetrievedDocument(String id, int rank) {
        this(id, rank, null);
    }
}
