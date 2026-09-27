package io.github.agentcheck.report;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.evaluation.MetricValue;
import io.github.agentcheck.regression.CountComparison;
import io.github.agentcheck.regression.MetricComparison;
import io.github.agentcheck.regression.RegressionResult;

import java.io.PrintStream;
import java.util.Locale;

/** Renders evaluation and regression results as human-readable console text. */
public final class ConsoleReporter {
    private static final String SECTION_SEPARATOR = "------------------------\n";
    private static final String REGRESSION_SEPARATOR = "--------------------------------------------------------\n";

    /** Creates a console reporter. */
    public ConsoleReporter() {
    }

    /**
     * Renders an evaluation result.
     *
     * @param result suite result
     * @return formatted console text
     */
    public String render(EvaluationSuiteResult result) {
        StringBuilder output = new StringBuilder();
        appendSuiteHeader(output, result);
        appendRetrievalSummary(output, result);
        appendToolSummary(output, result);
        appendPolicySummary(output, result);
        appendResultStatus(output, result.status());
        appendFailedCases(output, result);
        return output.toString();
    }

    /**
     * Prints an evaluation result.
     *
     * @param result suite result
     * @param output destination stream
     */
    public void print(EvaluationSuiteResult result, PrintStream output) {
        output.print(render(result));
    }

    /**
     * Renders a regression comparison.
     *
     * @param result regression result
     * @return formatted console text
     */
    public String render(RegressionResult result) {
        StringBuilder output = new StringBuilder();
        appendRegressionHeader(output, result);
        result.metrics().forEach(comparison -> appendMetricComparison(output, comparison));
        result.counts().forEach(comparison -> appendCountComparison(output, comparison));
        appendRegressionReasons(output, result);
        appendResultStatus(output, result.status());
        return output.toString();
    }

    /**
     * Prints a regression comparison.
     *
     * @param result regression result
     * @param output destination stream
     */
    public void print(RegressionResult result, PrintStream output) {
        output.print(render(result));
    }

    private void appendSuiteHeader(StringBuilder output, EvaluationSuiteResult result) {
        output.append("AgentCheck\n\nSuite: ")
                .append(result.suite())
                .append("\n\n")
                .append(result.cases()).append(" cases\n")
                .append(result.passed()).append(" passed\n")
                .append(result.failed()).append(" failed\n\n");
        if (result.skipped() > 0) {
            output.append(result.skipped()).append(" skipped\n\n");
        }
    }

    private void appendRetrievalSummary(StringBuilder output, EvaluationSuiteResult result) {
        appendSectionHeader(output, "Retrieval");
        output.append(String.format(
                        Locale.ROOT,
                        "Recall@%-14d %s%n",
                        result.retrieval().k(),
                        formatMetric(result.retrieval().recallAtK())))
                .append(String.format(
                        Locale.ROOT,
                        "%-22s %s%n%n",
                        "MRR",
                        formatMetric(result.retrieval().mrr())));
    }

    private void appendToolSummary(StringBuilder output, EvaluationSuiteResult result) {
        appendSectionHeader(output, "Tool behaviour");
        output.append(formatRow("Accuracy", formatMetric(result.tools().accuracy())))
                .append(formatRow("Missing", result.tools().missing()))
                .append(formatRow("Unexpected", result.tools().unexpected()))
                .append('\n');
    }

    private void appendPolicySummary(StringBuilder output, EvaluationSuiteResult result) {
        appendSectionHeader(output, "Policy");
        output.append(formatRow("Violations", result.policy().violations()));
    }

    private void appendFailedCases(StringBuilder output, EvaluationSuiteResult result) {
        result.caseResults().stream()
                .filter(caseResult -> caseResult.status() == EvaluationStatus.FAIL)
                .forEach(caseResult -> appendFailedCase(output, caseResult));
    }

    private void appendFailedCase(StringBuilder output, EvaluationCaseResult caseResult) {
        output.append("\nFAIL ").append(caseResult.caseId()).append("\n\n");
        caseResult.failureReasons().forEach(
                reason -> output.append("  ").append(reason).append('\n'));
    }

    private void appendRegressionHeader(StringBuilder output, RegressionResult result) {
        output.append("AgentCheck Regression\n\nSuite: ")
                .append(result.suite())
                .append("\n\n")
                .append(String.format(
                        Locale.ROOT,
                        "%-22s %10s %10s %10s%n",
                        "Metric",
                        "Baseline",
                        "Current",
                        "Change"))
                .append(REGRESSION_SEPARATOR);
    }

    private void appendMetricComparison(StringBuilder output, MetricComparison comparison) {
        output.append(String.format(
                Locale.ROOT,
                "%-22s %10s %10s %10s%n",
                comparison.metric(),
                formatMetric(comparison.baseline()),
                formatMetric(comparison.current()),
                formatChange(comparison.change())));
    }

    private void appendCountComparison(StringBuilder output, CountComparison comparison) {
        output.append(String.format(
                Locale.ROOT,
                "%-22s %10d %10d %+10d%n",
                comparison.metric(),
                comparison.baseline(),
                comparison.current(),
                comparison.change()));
    }

    private void appendRegressionReasons(StringBuilder output, RegressionResult result) {
        if (result.regressionReasons().isEmpty()) {
            return;
        }
        output.append("\nRegressions\n-----------\n");
        result.regressionReasons().forEach(
                reason -> output.append("- ").append(reason).append('\n'));
    }

    private void appendResultStatus(StringBuilder output, EvaluationStatus status) {
        output.append('\n').append(formatRow("Result", status));
    }

    private void appendSectionHeader(StringBuilder output, String title) {
        output.append(title).append('\n').append(SECTION_SEPARATOR);
    }

    private String formatRow(String label, Object value) {
        return String.format(Locale.ROOT, "%-22s %s%n", label, value);
    }

    private String formatMetric(MetricValue metric) {
        if (!metric.applicable()) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.2f", metric.value());
    }

    private String formatChange(Double change) {
        if (change == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%+.2f", change);
    }
}
