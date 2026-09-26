package io.github.agentcheck.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricValue(Double value, boolean applicable) {
    public static MetricValue of(double value) {
        return new MetricValue(value, true);
    }

    public static MetricValue notApplicable() {
        return new MetricValue(null, false);
    }
}
