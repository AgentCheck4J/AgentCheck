package io.github.agentcheck.regression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.agentcheck.evaluation.EvaluationStatus;

import java.util.List;

public record RegressionResult(
        String suite,
        List<MetricComparison> metrics,
        List<CountComparison> counts,
        EvaluationStatus status,
        List<String> regressionReasons) {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public RegressionResult {
        metrics = List.copyOf(metrics);
        counts = List.copyOf(counts);
        regressionReasons = List.copyOf(regressionReasons);
    }

    public String toJson() {
        try {
            return JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize regression result", exception);
        }
    }
}
