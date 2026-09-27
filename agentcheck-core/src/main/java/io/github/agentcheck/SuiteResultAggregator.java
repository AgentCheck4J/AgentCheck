package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.evaluation.MetricValue;
import io.github.agentcheck.evaluation.PolicySummary;
import io.github.agentcheck.evaluation.RetrievalSummary;
import io.github.agentcheck.evaluation.ToolSummary;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.golden.Thresholds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class SuiteResultAggregator {
    EvaluationSuiteResult aggregate(GoldenTestSuite suite, List<EvaluationCaseResult> caseResults) {
        MetricValue averageRecall = averageApplicableMetric(
                caseResults,
                result -> result.retrieval().recallAtK());
        MetricValue meanReciprocalRank = averageApplicableMetric(
                caseResults,
                result -> result.retrieval().reciprocalRank());
        MetricValue averageToolAccuracy = averageApplicableMetric(
                caseResults,
                result -> result.tools().accuracy());

        int missingTools = countMissingTools(caseResults);
        int unexpectedTools = countUnexpectedTools(caseResults);
        int policyViolations = countPolicyViolations(caseResults);
        int failedCases = countFailedCases(caseResults);
        List<String> failureReasons = createFailureReasons(
                suite,
                averageRecall,
                meanReciprocalRank,
                averageToolAccuracy,
                policyViolations,
                failedCases);

        return new EvaluationSuiteResult(
                suite.suite(),
                caseResults.size(),
                caseResults.size() - failedCases,
                failedCases,
                suite.cases().size() - caseResults.size(),
                new RetrievalSummary(suite.retrievalK(), averageRecall, meanReciprocalRank),
                new ToolSummary(averageToolAccuracy, missingTools, unexpectedTools),
                new PolicySummary(policyViolations),
                statusFor(failureReasons),
                failureReasons,
                caseResults);
    }

    private MetricValue averageApplicableMetric(
            List<EvaluationCaseResult> caseResults,
            Function<EvaluationCaseResult, MetricValue> metricExtractor) {
        List<MetricValue> applicableMetrics = caseResults.stream()
                .map(metricExtractor)
                .filter(MetricValue::applicable)
                .toList();
        if (applicableMetrics.isEmpty()) {
            return MetricValue.notApplicable();
        }

        double average = applicableMetrics.stream()
                .mapToDouble(MetricValue::value)
                .average()
                .orElseThrow();
        return MetricValue.of(average);
    }

    private int countMissingTools(List<EvaluationCaseResult> caseResults) {
        return caseResults.stream().mapToInt(result -> result.tools().missingRequired().size()).sum();
    }

    private int countUnexpectedTools(List<EvaluationCaseResult> caseResults) {
        return caseResults.stream().mapToInt(result -> result.tools().unexpectedCalled().size()).sum();
    }

    private int countPolicyViolations(List<EvaluationCaseResult> caseResults) {
        return caseResults.stream().mapToInt(result -> result.policyViolations().size()).sum();
    }

    private int countFailedCases(List<EvaluationCaseResult> caseResults) {
        return Math.toIntExact(caseResults.stream()
                .filter(result -> result.status() == EvaluationStatus.FAIL)
                .count());
    }

    private List<String> createFailureReasons(
            GoldenTestSuite suite,
            MetricValue averageRecall,
            MetricValue meanReciprocalRank,
            MetricValue averageToolAccuracy,
            int policyViolations,
            int failedCases) {
        List<String> failureReasons = new ArrayList<>();
        Thresholds thresholds = suite.thresholds();
        addThresholdFailure(
                failureReasons,
                "Recall@" + suite.retrievalK(),
                averageRecall,
                thresholds.recallAtK());
        addThresholdFailure(failureReasons, "MRR", meanReciprocalRank, thresholds.mrr());
        addThresholdFailure(failureReasons, "Tool accuracy", averageToolAccuracy, thresholds.toolAccuracy());
        addPolicyThresholdFailure(failureReasons, policyViolations, thresholds.maxPolicyViolations());
        addFailedCasesReason(failureReasons, failedCases);
        return List.copyOf(failureReasons);
    }

    private void addThresholdFailure(
            List<String> failureReasons,
            String metricName,
            MetricValue metric,
            Double threshold) {
        if (threshold != null && metric.applicable() && metric.value() < threshold) {
            failureReasons.add(
                    metricName + " " + format(metric.value()) + " is below threshold " + format(threshold));
        }
    }

    private void addPolicyThresholdFailure(
            List<String> failureReasons,
            int policyViolations,
            Integer maximumPolicyViolations) {
        if (maximumPolicyViolations != null && policyViolations > maximumPolicyViolations) {
            failureReasons.add(
                    "Policy violations " + policyViolations + " exceed maximum " + maximumPolicyViolations);
        }
    }

    private void addFailedCasesReason(List<String> failureReasons, int failedCases) {
        if (failedCases > 0) {
            failureReasons.add(failedCases + " case(s) failed behavioural checks");
        }
    }

    private EvaluationStatus statusFor(List<String> failureReasons) {
        return failureReasons.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
