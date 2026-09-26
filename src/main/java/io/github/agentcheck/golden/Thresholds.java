package io.github.agentcheck.golden;

public record Thresholds(Double recallAtK, Double mrr, Double toolAccuracy, Integer maxPolicyViolations) {
    public Thresholds {
        validateFraction(recallAtK, "retrieval recall threshold");
        validateFraction(mrr, "retrieval MRR threshold");
        validateFraction(toolAccuracy, "tool accuracy threshold");
        if (maxPolicyViolations != null && maxPolicyViolations < 0) {
            throw new IllegalArgumentException("maximum policy violations must not be negative");
        }
    }

    public static Thresholds defaults() {
        return new Thresholds(null, null, null, 0);
    }

    private static void validateFraction(Double value, String label) {
        if (value != null && (value < 0 || value > 1)) {
            throw new IllegalArgumentException(label + " must be between 0 and 1");
        }
    }
}
