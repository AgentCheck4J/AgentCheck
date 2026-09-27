package io.github.agentcheck.regression;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.agentcheck.evaluation.MetricValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricComparison(
        String metric,
        MetricValue baseline,
        MetricValue current,
        Double change,
        Double relativeChange,
        boolean regression) { }
