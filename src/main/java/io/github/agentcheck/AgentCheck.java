package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.evaluation.MetricValue;
import io.github.agentcheck.evaluation.PolicySummary;
import io.github.agentcheck.evaluation.RetrievalResult;
import io.github.agentcheck.evaluation.RetrievalSummary;
import io.github.agentcheck.evaluation.ToolResult;
import io.github.agentcheck.evaluation.ToolSummary;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.golden.Thresholds;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.policy.PolicyViolation;
import io.github.agentcheck.policy.PolicyViolationType;
import io.github.agentcheck.regression.CountComparison;
import io.github.agentcheck.regression.MetricComparison;
import io.github.agentcheck.regression.RegressionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

public final class AgentCheck {
    public EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution) {
        return evaluate(testCase, execution, 5);
    }

    public EvaluationSuiteResult evaluate(AgentAdapter agent, GoldenTestSuite suite) {
        Objects.requireNonNull(agent, "agent");
        Map<String, AgentExecution> executions = new LinkedHashMap<>();
        for (GoldenTestCase testCase : suite.cases().stream().filter(GoldenTestCase::enabled).toList()) {
            executions.put(testCase.id(), agent.execute(testCase.input()));
        }
        return evaluate(suite, executions);
    }

    public EvaluationSuiteResult evaluate(GoldenTestSuite suite, Map<String, AgentExecution> executionsByCaseId) {
        Objects.requireNonNull(suite, "suite");
        Objects.requireNonNull(executionsByCaseId, "executionsByCaseId");
        List<EvaluationCaseResult> results = suite.cases().stream().filter(GoldenTestCase::enabled).map(testCase -> {
            AgentExecution execution = executionsByCaseId.get(testCase.id());
            if (execution == null) throw new IllegalArgumentException("missing execution for case: " + testCase.id());
            return evaluate(testCase, execution, suite.retrievalK());
        }).toList();
        return aggregate(suite, results);
    }

    public RegressionResult compare(EvaluationSuiteResult baseline, EvaluationSuiteResult current) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(current, "current");
        validateComparable(baseline, current);

        List<MetricComparison> metrics = List.of(
                compareMetric("Recall@" + baseline.retrieval().k(), baseline.retrieval().recallAtK(), current.retrieval().recallAtK()),
                compareMetric("MRR", baseline.retrieval().mrr(), current.retrieval().mrr()),
                compareMetric("Tool accuracy", baseline.tools().accuracy(), current.tools().accuracy()));
        List<CountComparison> counts = List.of(
                compareCount("Failed cases", baseline.failed(), current.failed()),
                compareCount("Missing tools", baseline.tools().missing(), current.tools().missing()),
                compareCount("Unexpected tools", baseline.tools().unexpected(), current.tools().unexpected()),
                compareCount("Policy violations", baseline.policy().violations(), current.policy().violations()));
        List<String> reasons = new ArrayList<>();
        metrics.stream().filter(MetricComparison::regression)
                .map(this::metricRegressionReason)
                .forEach(reasons::add);
        counts.stream().filter(CountComparison::regression)
                .map(comparison -> comparison.metric() + " increased from " + comparison.baseline() + " to " + comparison.current())
                .forEach(reasons::add);
        return new RegressionResult(
                baseline.suite(), metrics, counts,
                reasons.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL,
                reasons);
    }

    private EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution, int k) {
        Objects.requireNonNull(testCase, "testCase");
        Objects.requireNonNull(execution, "execution");
        if (!testCase.input().equals(execution.input())) {
            throw new IllegalArgumentException("execution input does not match case '" + testCase.id() + "'");
        }

        RetrievalResult retrieval = evaluateRetrieval(testCase, execution, k);
        ToolResult tools = evaluateTools(testCase, execution);
        List<PolicyViolation> violations = tools.forbiddenCalled().stream()
                .map(tool -> new PolicyViolation(PolicyViolationType.FORBIDDEN_TOOL_CALL, tool,
                        "Forbidden tool was called: " + tool))
                .toList();
        List<String> reasons = new ArrayList<>();
        if (retrieval.recallAtK().applicable() && retrieval.recallAtK().value() < 1.0) {
            reasons.add("Not all relevant documents were retrieved in the top " + k);
        }
        tools.missingRequired().forEach(tool -> reasons.add("Missing required tool: " + tool));
        tools.unexpectedCalled().forEach(tool -> reasons.add("Unexpected tool: " + tool));
        tools.forbiddenCalled().forEach(tool -> reasons.add("Forbidden tool: " + tool));
        EvaluationStatus status = reasons.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationCaseResult(testCase.id(), retrieval, tools, violations, status, reasons);
    }

    private RetrievalResult evaluateRetrieval(GoldenTestCase testCase, AgentExecution execution, int k) {
        Set<String> relevant = new LinkedHashSet<>(testCase.expected().relevantDocuments());
        if (relevant.isEmpty()) {
            return new RetrievalResult(k, MetricValue.notApplicable(), MetricValue.notApplicable(), List.of());
        }
        Map<String, Integer> rankedUnique = execution.retrievedDocuments().stream()
                .sorted(Comparator.comparingInt(document -> document.rank()))
                .collect(LinkedHashMap<String, Integer>::new,
                        (items, document) -> items.putIfAbsent(document.id(), document.rank()),
                        LinkedHashMap::putAll);
        List<String> topK = rankedUnique.entrySet().stream()
                .filter(entry -> entry.getValue() <= k)
                .map(Map.Entry::getKey)
                .toList();
        List<String> matches = topK.stream().filter(relevant::contains).toList();
        double recall = (double) matches.size() / relevant.size();
        OptionalInt firstRelevantRank = rankedUnique.entrySet().stream()
                .filter(entry -> relevant.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .min();
        double reciprocalRank = firstRelevantRank.isPresent() ? 1.0 / firstRelevantRank.getAsInt() : 0.0;
        return new RetrievalResult(k, MetricValue.of(recall), MetricValue.of(reciprocalRank), matches);
    }

    private ToolResult evaluateTools(GoldenTestCase testCase, AgentExecution execution) {
        Set<String> required = new LinkedHashSet<>(testCase.expected().requiredTools());
        Set<String> forbidden = new LinkedHashSet<>(testCase.expected().forbiddenTools());
        if (required.isEmpty() && forbidden.isEmpty()) {
            return new ToolResult(MetricValue.notApplicable(), List.of(), List.of(), List.of(), List.of());
        }
        Set<String> called = execution.toolCalls().stream()
                .map(call -> call.name())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> calledRequired = required.stream().filter(called::contains).toList();
        List<String> missingRequired = required.stream().filter(tool -> !called.contains(tool)).toList();
        List<String> forbiddenCalled = forbidden.stream().filter(called::contains).toList();
        List<String> unexpected = called.stream().filter(tool -> !required.contains(tool) && !forbidden.contains(tool)).toList();
        int correct = calledRequired.size() + forbidden.size() - forbiddenCalled.size();
        int checks = required.size() + forbidden.size() + unexpected.size();
        return new ToolResult(MetricValue.of((double) correct / checks), calledRequired, missingRequired, unexpected, forbiddenCalled);
    }

    private EvaluationSuiteResult aggregate(GoldenTestSuite suite, List<EvaluationCaseResult> results) {
        List<EvaluationCaseResult> retrievalResults = results.stream().filter(result -> result.retrieval().recallAtK().applicable()).toList();
        MetricValue recall = retrievalResults.isEmpty() ? MetricValue.notApplicable() : MetricValue.of(
                retrievalResults.stream().mapToDouble(result -> result.retrieval().recallAtK().value()).average().orElseThrow());
        MetricValue mrr = retrievalResults.isEmpty() ? MetricValue.notApplicable() : MetricValue.of(
                retrievalResults.stream().mapToDouble(result -> result.retrieval().reciprocalRank().value()).average().orElseThrow());

        List<EvaluationCaseResult> toolResults = results.stream().filter(result -> result.tools().accuracy().applicable()).toList();
        MetricValue toolAccuracy = toolResults.isEmpty() ? MetricValue.notApplicable() : MetricValue.of(
                toolResults.stream().mapToDouble(result -> result.tools().accuracy().value()).average().orElseThrow());
        int missing = results.stream().mapToInt(result -> result.tools().missingRequired().size()).sum();
        int unexpected = results.stream().mapToInt(result -> result.tools().unexpectedCalled().size()).sum();
        int violations = results.stream().mapToInt(result -> result.policyViolations().size()).sum();

        List<String> failures = new ArrayList<>();
        Thresholds thresholds = suite.thresholds();
        thresholdFailure(failures, "Recall@" + suite.retrievalK(), recall, thresholds.recallAtK());
        thresholdFailure(failures, "MRR", mrr, thresholds.mrr());
        thresholdFailure(failures, "Tool accuracy", toolAccuracy, thresholds.toolAccuracy());
        if (thresholds.maxPolicyViolations() != null && violations > thresholds.maxPolicyViolations()) {
            failures.add("Policy violations " + violations + " exceed maximum " + thresholds.maxPolicyViolations());
        }
        int failedCases = (int) results.stream().filter(result -> result.status() == EvaluationStatus.FAIL).count();
        if (failedCases > 0) failures.add(failedCases + " case(s) failed behavioural checks");
        EvaluationStatus status = failures.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationSuiteResult(
                suite.suite(), results.size(), results.size() - failedCases, failedCases,
                suite.cases().size() - results.size(),
                new RetrievalSummary(suite.retrievalK(), recall, mrr),
                new ToolSummary(toolAccuracy, missing, unexpected),
                new PolicySummary(violations), status, failures, results);
    }

    private void thresholdFailure(List<String> failures, String name, MetricValue metric, Double threshold) {
        if (threshold != null && metric.applicable() && metric.value() < threshold) {
            failures.add(name + " " + format(metric.value()) + " is below threshold " + format(threshold));
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void validateComparable(EvaluationSuiteResult baseline, EvaluationSuiteResult current) {
        if (!baseline.suite().equals(current.suite())) {
            throw new IllegalArgumentException("cannot compare different suites: '" + baseline.suite() + "' and '" + current.suite() + "'");
        }
        if (baseline.retrieval().k() != current.retrieval().k()) {
            throw new IllegalArgumentException("cannot compare results with different retrieval cutoffs");
        }
        Set<String> baselineCases = baseline.caseResults().stream().map(EvaluationCaseResult::caseId).collect(Collectors.toSet());
        Set<String> currentCases = current.caseResults().stream().map(EvaluationCaseResult::caseId).collect(Collectors.toSet());
        if (!baselineCases.equals(currentCases)) {
            throw new IllegalArgumentException("cannot compare results with different case IDs");
        }
    }

    private MetricComparison compareMetric(String name, MetricValue baseline, MetricValue current) {
        boolean regression = baseline.applicable() && (!current.applicable() || current.value() < baseline.value());
        if (!baseline.applicable() || !current.applicable()) {
            return new MetricComparison(name, baseline, current, null, null, regression);
        }
        double change = current.value() - baseline.value();
        Double relativeChange = baseline.value() == 0.0 ? null : change / baseline.value();
        return new MetricComparison(name, baseline, current, change, relativeChange, regression);
    }

    private CountComparison compareCount(String name, int baseline, int current) {
        return new CountComparison(name, baseline, current, current - baseline, current > baseline);
    }

    private String metricRegressionReason(MetricComparison comparison) {
        if (!comparison.current().applicable()) {
            return comparison.metric() + " became not applicable";
        }
        String percent = comparison.relativeChange() == null
                ? ""
                : " by " + String.format(Locale.ROOT, "%.1f%%", Math.abs(comparison.relativeChange()) * 100);
        return comparison.metric() + " decreased" + percent + " ("
                + format(comparison.baseline().value()) + " -> " + format(comparison.current().value()) + ")";
    }
}
