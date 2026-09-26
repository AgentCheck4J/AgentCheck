package io.github.agentcheck.report;

import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.evaluation.MetricValue;

import java.io.PrintStream;
import java.util.Locale;

public final class ConsoleReporter {
    public String render(EvaluationSuiteResult result) {
        var output = new StringBuilder();
        output.append("AgentCheck\n\nSuite: ").append(result.suite()).append("\n\n")
                .append(result.cases()).append(" cases\n")
                .append(result.passed()).append(" passed\n")
                .append(result.failed()).append(" failed\n\n")
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

    private String format(MetricValue metric) {
        return metric.applicable() ? String.format(Locale.ROOT, "%.2f", metric.value()) : "N/A";
    }
}
