package io.github.agentcheck.golden;

import java.util.List;

public record ExpectedBehaviour(
        List<String> relevantDocuments,
        List<String> requiredTools,
        List<String> forbiddenTools) {

    public ExpectedBehaviour {
        relevantDocuments = immutableDistinct(relevantDocuments, "relevant document");
        requiredTools = immutableDistinct(requiredTools, "required tool");
        forbiddenTools = immutableDistinct(forbiddenTools, "forbidden tool");
        var overlap = requiredTools.stream().filter(forbiddenTools::contains).findFirst();
        if (overlap.isPresent()) {
            throw new IllegalArgumentException("tool cannot be both required and forbidden: " + overlap.get());
        }
    }

    public static ExpectedBehaviour none() {
        return new ExpectedBehaviour(List.of(), List.of(), List.of());
    }

    private static List<String> immutableDistinct(List<String> values, String label) {
        if (values == null) return List.of();
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return values.stream().distinct().toList();
    }
}
