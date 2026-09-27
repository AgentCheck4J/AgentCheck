package io.github.agentcheck.regression;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.agentcheck.evaluation.MetricValue;

/**
 * Baseline/current comparison of one numeric metric.
 *
 * @param metric metric name
 * @param baseline baseline metric
 * @param current current metric
 * @param change absolute current-minus-baseline change, or {@code null}
 * @param relativeChange relative change, or {@code null} when undefined
 * @param regression whether the comparison is a regression
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricComparison(
        String metric,
        MetricValue baseline,
        MetricValue current,
        Double change,
        Double relativeChange,
        boolean regression) { }
