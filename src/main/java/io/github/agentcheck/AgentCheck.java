package io.github.agentcheck;

import io.github.agentcheck.evaluation.*;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.policy.PolicyViolation;
import io.github.agentcheck.policy.PolicyViolationType;

import java.util.*;

public final class AgentCheck {
    public EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution) {
        return evaluate(testCase, execution, 5);
    }

    public EvaluationSuiteResult evaluate(AgentAdapter agent, GoldenTestSuite suite) {
        Objects.requireNonNull(agent, "agent");
        var executions = new LinkedHashMap<String, AgentExecution>();
        for (var testCase : suite.cases()) {
            executions.put(testCase.id(), agent.execute(testCase.input()));
        }
        return evaluate(suite, executions);
    }

    public EvaluationSuiteResult evaluate(GoldenTestSuite suite, Map<String, AgentExecution> executionsByCaseId) {
        Objects.requireNonNull(suite, "suite");
        Objects.requireNonNull(executionsByCaseId, "executionsByCaseId");
        var results = suite.cases().stream().map(testCase -> {
            var execution = executionsByCaseId.get(testCase.id());
            if (execution == null) throw new IllegalArgumentException("missing execution for case: " + testCase.id());
            return evaluate(testCase, execution, suite.retrievalK());
        }).toList();
        return aggregate(suite, results);
    }

    private EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution, int k) {
        Objects.requireNonNull(testCase, "testCase");
        Objects.requireNonNull(execution, "execution");
        if (!testCase.input().equals(execution.input())) {
            throw new IllegalArgumentException("execution input does not match case '" + testCase.id() + "'");
        }

        var retrieval = evaluateRetrieval(testCase, execution, k);
        var tools = evaluateTools(testCase, execution);
        var violations = tools.forbiddenCalled().stream()
                .map(tool -> new PolicyViolation(PolicyViolationType.FORBIDDEN_TOOL_CALL, tool,
                        "Forbidden tool was called: " + tool))
                .toList();
        var reasons = new ArrayList<String>();
        if (retrieval.recallAtK().applicable() && retrieval.recallAtK().value() < 1.0) {
            reasons.add("Not all relevant documents were retrieved in the top " + k);
        }
        tools.missingRequired().forEach(tool -> reasons.add("Missing required tool: " + tool));
        tools.unexpectedCalled().forEach(tool -> reasons.add("Unexpected tool: " + tool));
        tools.forbiddenCalled().forEach(tool -> reasons.add("Forbidden tool: " + tool));
        var status = reasons.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationCaseResult(testCase.id(), retrieval, tools, violations, status, reasons);
    }

    private RetrievalResult evaluateRetrieval(GoldenTestCase testCase, AgentExecution execution, int k) {
        var relevant = new LinkedHashSet<>(testCase.expected().relevantDocuments());
        if (relevant.isEmpty()) {
            return new RetrievalResult(k, MetricValue.notApplicable(), MetricValue.notApplicable(), List.of());
        }
        var rankedUnique = execution.retrievedDocuments().stream()
                .sorted(Comparator.comparingInt(document -> document.rank()))
                .collect(LinkedHashMap<String, Integer>::new,
                        (items, document) -> items.putIfAbsent(document.id(), document.rank()),
                        LinkedHashMap::putAll);
        var topK = rankedUnique.entrySet().stream()
                .filter(entry -> entry.getValue() <= k)
                .map(Map.Entry::getKey)
                .toList();
        var matches = topK.stream().filter(relevant::contains).toList();
        var recall = (double) matches.size() / relevant.size();
        var firstRelevantRank = rankedUnique.entrySet().stream()
                .filter(entry -> relevant.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .min();
        var reciprocalRank = firstRelevantRank.isPresent() ? 1.0 / firstRelevantRank.getAsInt() : 0.0;
        return new RetrievalResult(k, MetricValue.of(recall), MetricValue.of(reciprocalRank), matches);
    }

    private ToolResult evaluateTools(GoldenTestCase testCase, AgentExecution execution) {
        var required = new LinkedHashSet<>(testCase.expected().requiredTools());
        var forbidden = new LinkedHashSet<>(testCase.expected().forbiddenTools());
        if (required.isEmpty() && forbidden.isEmpty()) {
            return new ToolResult(MetricValue.notApplicable(), List.of(), List.of(), List.of(), List.of());
        }
        var called = execution.toolCalls().stream()
                .map(call -> call.name())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        var calledRequired = required.stream().filter(called::contains).toList();
        var missingRequired = required.stream().filter(tool -> !called.contains(tool)).toList();
        var forbiddenCalled = forbidden.stream().filter(called::contains).toList();
        var unexpected = called.stream().filter(tool -> !required.contains(tool) && !forbidden.contains(tool)).toList();
        var correct = calledRequired.size() + forbidden.size() - forbiddenCalled.size();
        var checks = required.size() + forbidden.size() + unexpected.size();
        return new ToolResult(MetricValue.of((double) correct / checks), calledRequired, missingRequired, unexpected, forbiddenCalled);
    }

    private EvaluationSuiteResult aggregate(GoldenTestSuite suite, List<EvaluationCaseResult> results) {
        var retrievalResults = results.stream().filter(result -> result.retrieval().recallAtK().applicable()).toList();
        var recall = retrievalResults.isEmpty() ? MetricValue.notApplicable() : MetricValue.of(
                retrievalResults.stream().mapToDouble(result -> result.retrieval().recallAtK().value()).average().orElseThrow());
        var mrr = retrievalResults.isEmpty() ? MetricValue.notApplicable() : MetricValue.of(
                retrievalResults.stream().mapToDouble(result -> result.retrieval().reciprocalRank().value()).average().orElseThrow());

        var toolResults = results.stream().filter(result -> result.tools().accuracy().applicable()).toList();
        var toolAccuracy = toolResults.isEmpty() ? MetricValue.notApplicable() : MetricValue.of(
                toolResults.stream().mapToDouble(result -> result.tools().accuracy().value()).average().orElseThrow());
        var missing = results.stream().mapToInt(result -> result.tools().missingRequired().size()).sum();
        var unexpected = results.stream().mapToInt(result -> result.tools().unexpectedCalled().size()).sum();
        var violations = results.stream().mapToInt(result -> result.policyViolations().size()).sum();

        var failures = new ArrayList<String>();
        var thresholds = suite.thresholds();
        thresholdFailure(failures, "Recall@" + suite.retrievalK(), recall, thresholds.recallAtK());
        thresholdFailure(failures, "MRR", mrr, thresholds.mrr());
        thresholdFailure(failures, "Tool accuracy", toolAccuracy, thresholds.toolAccuracy());
        if (thresholds.maxPolicyViolations() != null && violations > thresholds.maxPolicyViolations()) {
            failures.add("Policy violations " + violations + " exceed maximum " + thresholds.maxPolicyViolations());
        }
        var failedCases = (int) results.stream().filter(result -> result.status() == EvaluationStatus.FAIL).count();
        if (failedCases > 0) failures.add(failedCases + " case(s) failed behavioural checks");
        var status = failures.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationSuiteResult(
                suite.suite(), results.size(), results.size() - failedCases, failedCases,
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
}
