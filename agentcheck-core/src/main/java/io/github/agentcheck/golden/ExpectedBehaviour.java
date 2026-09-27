package io.github.agentcheck.golden;

import java.util.List;
import java.util.Optional;

/**
 * Expected retrieval and tool behaviour for one golden test case.
 *
 * @param relevantDocuments document IDs that should be retrieved
 * @param requiredTools tools that must be called
 * @param forbiddenTools tools that must not be called
 */
public record ExpectedBehaviour(
        List<String> relevantDocuments,
        List<String> requiredTools,
        List<String> forbiddenTools) {

    /** Validates, normalizes, and defensively copies the expectations. */
    public ExpectedBehaviour {
        relevantDocuments = normalizeUniqueValues(relevantDocuments, "relevant document");
        requiredTools = normalizeUniqueValues(requiredTools, "required tool");
        forbiddenTools = normalizeUniqueValues(forbiddenTools, "forbidden tool");
        requireDisjointToolExpectations(requiredTools, forbiddenTools);
    }

    /**
     * Creates an expectation without retrieval or tool assertions.
     *
     * @return empty expected behaviour
     */
    public static ExpectedBehaviour none() {
        return new ExpectedBehaviour(List.of(), List.of(), List.of());
    }

    private static List<String> normalizeUniqueValues(List<String> values, String valueDescription) {
        if (values == null) {
            return List.of();
        }
        boolean containsBlankValue = values.stream().anyMatch(value -> value == null || value.isBlank());
        if (containsBlankValue) {
            throw new IllegalArgumentException(valueDescription + " must not be blank");
        }
        return values.stream().distinct().toList();
    }

    private static void requireDisjointToolExpectations(
            List<String> requiredTools,
            List<String> forbiddenTools) {
        Optional<String> conflictingTool = requiredTools.stream()
                .filter(forbiddenTools::contains)
                .findFirst();
        if (conflictingTool.isPresent()) {
            throw new IllegalArgumentException(
                    "tool cannot be both required and forbidden: " + conflictingTool.get());
        }
    }
}
