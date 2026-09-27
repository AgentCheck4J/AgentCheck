package io.github.agentcheck.golden;

/**
 * Optional aggregate thresholds applied to a suite result.
 *
 * @param recallAtK minimum mean recall at the configured cutoff
 * @param mrr minimum mean reciprocal rank
 * @param toolAccuracy minimum mean tool accuracy
 * @param maxPolicyViolations maximum allowed policy violations
 */
public record Thresholds(Double recallAtK, Double mrr, Double toolAccuracy, Integer maxPolicyViolations) {
    /** Validates all configured thresholds. */
    public Thresholds {
        validateFraction(recallAtK, "retrieval recall threshold");
        validateFraction(mrr, "retrieval MRR threshold");
        validateFraction(toolAccuracy, "tool accuracy threshold");
        if (maxPolicyViolations != null && maxPolicyViolations < 0) {
            throw new IllegalArgumentException("maximum policy violations must not be negative");
        }
    }

    /**
     * Creates thresholds that only reject policy violations.
     *
     * @return default thresholds
     */
    public static Thresholds defaults() {
        return new Thresholds(null, null, null, 0);
    }

    private static void validateFraction(Double value, String label) {
        if (value != null && (value < 0 || value > 1)) {
            throw new IllegalArgumentException(label + " must be between 0 and 1");
        }
    }
}
