package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.evaluation.MetricValue;
import io.github.agentcheck.regression.CountComparison;
import io.github.agentcheck.regression.MetricComparison;
import io.github.agentcheck.regression.RegressionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class RegressionComparator {
    RegressionResult compare(EvaluationSuiteResult baseline, EvaluationSuiteResult current) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(current, "current");
        requireComparableResults(baseline, current);

        List<MetricComparison> metricComparisons = compareMetrics(baseline, current);
        List<CountComparison> countComparisons = compareCounts(baseline, current);
        List<String> regressionReasons = createRegressionReasons(metricComparisons, countComparisons);

        return new RegressionResult(
                baseline.suite(),
                metricComparisons,
                countComparisons,
                statusFor(regressionReasons),
                regressionReasons);
    }

    private List<MetricComparison> compareMetrics(
            EvaluationSuiteResult baseline,
            EvaluationSuiteResult current) {
        return List.of(
                compareMetric(
                        "Recall@" + baseline.retrieval().k(),
                        baseline.retrieval().recallAtK(),
                        current.retrieval().recallAtK()),
                compareMetric("MRR", baseline.retrieval().mrr(), current.retrieval().mrr()),
                compareMetric("Tool accuracy", baseline.tools().accuracy(), current.tools().accuracy()));
    }

    private List<CountComparison> compareCounts(
            EvaluationSuiteResult baseline,
            EvaluationSuiteResult current) {
        return List.of(
                compareCount("Failed cases", baseline.failed(), current.failed()),
                compareCount("Missing tools", baseline.tools().missing(), current.tools().missing()),
                compareCount("Unexpected tools", baseline.tools().unexpected(), current.tools().unexpected()),
                compareCount("Policy violations", baseline.policy().violations(), current.policy().violations()));
    }

    private List<String> createRegressionReasons(
            List<MetricComparison> metricComparisons,
            List<CountComparison> countComparisons) {
        List<String> regressionReasons = new ArrayList<>();
        metricComparisons.stream()
                .filter(MetricComparison::regression)
                .map(this::describeMetricRegression)
                .forEach(regressionReasons::add);
        countComparisons.stream()
                .filter(CountComparison::regression)
                .map(this::describeCountRegression)
                .forEach(regressionReasons::add);
        return List.copyOf(regressionReasons);
    }

    private MetricComparison compareMetric(String metricName, MetricValue baseline, MetricValue current) {
        boolean regression = isMetricRegression(baseline, current);
        if (!baseline.applicable() || !current.applicable()) {
            return new MetricComparison(metricName, baseline, current, null, null, regression);
        }

        double absoluteChange = current.value() - baseline.value();
        Double relativeChange = baseline.value() == 0.0 ? null : absoluteChange / baseline.value();
        return new MetricComparison(
                metricName, baseline, current, absoluteChange, relativeChange, regression);
    }

    private boolean isMetricRegression(MetricValue baseline, MetricValue current) {
        return baseline.applicable() && (!current.applicable() || current.value() < baseline.value());
    }

    private CountComparison compareCount(String metricName, int baseline, int current) {
        return new CountComparison(metricName, baseline, current, current - baseline, current > baseline);
    }

    private String describeMetricRegression(MetricComparison comparison) {
        if (!comparison.current().applicable()) {
            return comparison.metric() + " became not applicable";
        }
        return comparison.metric() + " decreased" + formatRelativeDecrease(comparison.relativeChange()) + " ("
                + format(comparison.baseline().value()) + " -> " + format(comparison.current().value()) + ")";
    }

    private String formatRelativeDecrease(Double relativeChange) {
        if (relativeChange == null) {
            return "";
        }
        return " by " + String.format(Locale.ROOT, "%.1f%%", Math.abs(relativeChange) * 100);
    }

    private String describeCountRegression(CountComparison comparison) {
        return comparison.metric() + " increased from " + comparison.baseline() + " to " + comparison.current();
    }

    private void requireComparableResults(
            EvaluationSuiteResult baseline,
            EvaluationSuiteResult current) {
        requireSameSuite(baseline, current);
        requireSameRetrievalCutoff(baseline, current);
        requireSameCaseIds(baseline, current);
    }

    private void requireSameSuite(EvaluationSuiteResult baseline, EvaluationSuiteResult current) {
        if (!baseline.suite().equals(current.suite())) {
            throw new IllegalArgumentException(
                    "cannot compare different suites: '" + baseline.suite() + "' and '" + current.suite() + "'");
        }
    }

    private void requireSameRetrievalCutoff(
            EvaluationSuiteResult baseline,
            EvaluationSuiteResult current) {
        if (baseline.retrieval().k() != current.retrieval().k()) {
            throw new IllegalArgumentException("cannot compare results with different retrieval cutoffs");
        }
    }

    private void requireSameCaseIds(EvaluationSuiteResult baseline, EvaluationSuiteResult current) {
        Set<String> baselineCaseIds = extractCaseIds(baseline);
        Set<String> currentCaseIds = extractCaseIds(current);
        if (!baselineCaseIds.equals(currentCaseIds)) {
            throw new IllegalArgumentException("cannot compare results with different case IDs");
        }
    }

    private Set<String> extractCaseIds(EvaluationSuiteResult result) {
        return result.caseResults().stream()
                .map(EvaluationCaseResult::caseId)
                .collect(Collectors.toSet());
    }

    private EvaluationStatus statusFor(List<String> regressionReasons) {
        return regressionReasons.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
