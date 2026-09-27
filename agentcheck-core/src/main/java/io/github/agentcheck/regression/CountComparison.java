package io.github.agentcheck.regression;

/**
 * Baseline/current comparison of one integer count.
 *
 * @param metric metric name
 * @param baseline baseline count
 * @param current current count
 * @param change current count minus baseline count
 * @param regression whether the change is a regression
 */
public record CountComparison(
        String metric,
        int baseline,
        int current,
        int change,
        boolean regression) { }
