package io.github.agentcheck.model;

public record RetrievedDocument(String id, int rank, Double score) {
    public RetrievedDocument {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("document id must not be blank");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("document rank must be at least 1");
        }
    }

    public RetrievedDocument(String id, int rank) {
        this(id, rank, null);
    }
}
