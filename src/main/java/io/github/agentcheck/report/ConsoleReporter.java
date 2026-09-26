package io.github.agentcheck.report;

import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.evaluation.MetricValue;
import io.github.agentcheck.regression.RegressionResult;

import java.io.PrintStream;
import java.util.Locale;

public final class ConsoleReporter {
    public String render(EvaluationSuiteResult result) {
        StringBuilder output = new StringBuilder();
        output.append("AgentCheck\n\nSuite: ").append(result.suite()).append("\n\n")
                .append(result.cases()).append(" cases\n")
                .append(result.passed()).append(" passed\n")
                .append(result.failed()).append(" failed\n\n")
                .append(result.skipped() == 0 ? "" : result.skipped() + " skipped\n\n")
                .append("Retrieval\n------------------------\n")
                .append(String.format(Locale.ROOT, "Recall@%-14d %s%n", result.retrieval().k(), format(result.retrieval().recallAtK())))
                .append(String.format(Locale.ROOT, "%-22s %s%n%n", "MRR", format(result.retrieval().mrr())))
                .append("Tool behaviour\n------------------------\n")
                .append(String.format(Locale.ROOT, "%-22s %s%n", "Accuracy", format(result.tools().accuracy())))
                .append(String.format(Locale.ROOT, "%-22s %d%n", "Missing", result.tools().missing()))
                .append(String.format(Locale.ROOT, "%-22s %d%n%n", "Unexpected", result.tools().unexpected()))
                .append("Policy\n------------------------\n")
                .append(String.format(Locale.ROOT, "%-22s %d%n%n", "Violations", result.policy().violations()))
                .append(String.format(Locale.ROOT, "%-22s %s%n", "Result", result.status()));

        result.caseResults().stream().filter(caseResult -> caseResult.status() == EvaluationStatus.FAIL).forEach(caseResult -> {
            output.append("\nFAIL ").append(caseResult.caseId()).append("\n\n");
            caseResult.failureReasons().forEach(reason -> output.append("  ").append(reason).append("\n"));
        });
        return output.toString();
    }

    public void print(EvaluationSuiteResult result, PrintStream output) {
        output.print(render(result));
    }

    public String render(RegressionResult result) {
        StringBuilder output = new StringBuilder("AgentCheck Regression\n\nSuite: ")
                .append(result.suite())
                .append("\n\n")
                .append(String.format(Locale.ROOT, "%-22s %10s %10s %10s%n", "Metric", "Baseline", "Current", "Change"))
                .append("--------------------------------------------------------\n");
        result.metrics().forEach(metric -> output.append(String.format(Locale.ROOT, "%-22s %10s %10s %10s%n",
                metric.metric(), format(metric.baseline()), format(metric.current()), formatChange(metric.change()))));
        result.counts().forEach(metric -> output.append(String.format(Locale.ROOT, "%-22s %10d %10d %+10d%n",
                metric.metric(), metric.baseline(), metric.current(), metric.change())));
        if (!result.regressionReasons().isEmpty()) {
            output.append("\nRegressions\n-----------\n");
            result.regressionReasons().forEach(reason -> output.append("- ").append(reason).append('\n'));
        }
        output.append('\n').append(String.format(Locale.ROOT, "%-22s %s%n", "Result", result.status()));
        return output.toString();
    }

    public void print(RegressionResult result, PrintStream output) {
        output.print(render(result));
    }

    private String format(MetricValue metric) {
        return metric.applicable() ? String.format(Locale.ROOT, "%.2f", metric.value()) : "N/A";
    }

    private String formatChange(Double change) {
        return change == null ? "N/A" : String.format(Locale.ROOT, "%+.2f", change);
    }
}
