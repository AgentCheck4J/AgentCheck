package io.github.agentcheck.recording;

/**
 * Controls golden-draft output. The defaults omit answers and tool arguments
 * because both may contain sensitive data.
 *
 * @param retrievalK retrieval cutoff written to the draft
 * @param includeObservedAnswers whether observed answer text is serialized
 * @param includeObservedToolArguments whether observed tool arguments are serialized
 */
public record RecordingOptions(
        int retrievalK,
        boolean includeObservedAnswers,
        boolean includeObservedToolArguments) {

    private static final int DEFAULT_RETRIEVAL_CUTOFF = 5;

    /** Validates the configured retrieval cutoff. */
    public RecordingOptions {
        if (retrievalK < 1) {
            throw new IllegalArgumentException("retrieval k must be at least 1");
        }
    }

    /**
     * Returns options with retrieval cutoff 5 and sensitive observations off.
     *
     * @return privacy-preserving default options
     */
    public static RecordingOptions defaults() {
        return new RecordingOptions(DEFAULT_RETRIEVAL_CUTOFF, false, false);
    }
}
