package io.github.agentcheck.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metric value that explicitly distinguishes a numeric result from an inapplicable metric.
 *
 * @param value numeric value, or {@code null} when not applicable
 * @param applicable whether the metric applies to the evaluated data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricValue(Double value, boolean applicable) {
    /**
     * Creates an applicable metric.
     *
     * @param value calculated metric value
     * @return applicable metric
     */
    public static MetricValue of(double value) {
        return new MetricValue(value, true);
    }

    /**
     * Creates a metric that does not apply to the evaluated data.
     *
     * @return inapplicable metric
     */
    public static MetricValue notApplicable() {
        return new MetricValue(null, false);
    }
}
