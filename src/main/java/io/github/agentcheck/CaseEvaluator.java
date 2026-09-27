package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.MetricValue;
import io.github.agentcheck.evaluation.RetrievalResult;
import io.github.agentcheck.evaluation.ToolResult;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import io.github.agentcheck.policy.PolicyViolation;
import io.github.agentcheck.policy.PolicyViolationType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

final class CaseEvaluator {
    EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution, int retrievalCutoff) {
        Objects.requireNonNull(testCase, "testCase");
        Objects.requireNonNull(execution, "execution");
        requireMatchingInput(testCase, execution);

        RetrievalResult retrievalResult = evaluateRetrieval(testCase, execution, retrievalCutoff);
        ToolResult toolResult = evaluateTools(testCase, execution);
        List<PolicyViolation> policyViolations = createPolicyViolations(toolResult);
        List<String> failureReasons = createFailureReasons(retrievalResult, toolResult, retrievalCutoff);

        return new EvaluationCaseResult(
                testCase.id(), retrievalResult, toolResult, policyViolations,
                statusFor(failureReasons), failureReasons);
    }

    private void requireMatchingInput(GoldenTestCase testCase, AgentExecution execution) {
        if (!testCase.input().equals(execution.input())) {
            throw new IllegalArgumentException("execution input does not match case '" + testCase.id() + "'");
        }
    }

    private RetrievalResult evaluateRetrieval(
            GoldenTestCase testCase,
            AgentExecution execution,
            int retrievalCutoff) {
        Set<String> relevantDocumentIds = new LinkedHashSet<>(testCase.expected().relevantDocuments());
        if (relevantDocumentIds.isEmpty()) {
            return noRetrievalEvaluation(retrievalCutoff);
        }

        Map<String, Integer> rankByDocumentId = rankUniqueDocuments(execution.retrievedDocuments());
        List<String> matchedDocumentIds = findTopMatches(
                rankByDocumentId, relevantDocumentIds, retrievalCutoff);
        double recall = (double) matchedDocumentIds.size() / relevantDocumentIds.size();
        double reciprocalRank = calculateReciprocalRank(rankByDocumentId, relevantDocumentIds);

        return new RetrievalResult(
                retrievalCutoff, MetricValue.of(recall), MetricValue.of(reciprocalRank), matchedDocumentIds);
    }

    private RetrievalResult noRetrievalEvaluation(int retrievalCutoff) {
        return new RetrievalResult(
                retrievalCutoff, MetricValue.notApplicable(), MetricValue.notApplicable(), List.of());
    }

    private Map<String, Integer> rankUniqueDocuments(List<RetrievedDocument> documents) {
        return documents.stream()
                .sorted(Comparator.comparingInt(RetrievedDocument::rank))
                .collect(
                        LinkedHashMap<String, Integer>::new,
                        (rankById, document) -> rankById.putIfAbsent(document.id(), document.rank()),
                        LinkedHashMap::putAll);
    }

    private List<String> findTopMatches(
            Map<String, Integer> rankByDocumentId,
            Set<String> relevantDocumentIds,
            int retrievalCutoff) {
        return rankByDocumentId.entrySet().stream()
                .filter(entry -> entry.getValue() <= retrievalCutoff)
                .map(Map.Entry::getKey)
                .filter(relevantDocumentIds::contains)
                .toList();
    }

    private double calculateReciprocalRank(
            Map<String, Integer> rankByDocumentId,
            Set<String> relevantDocumentIds) {
        OptionalInt firstRelevantRank = rankByDocumentId.entrySet().stream()
                .filter(entry -> relevantDocumentIds.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .min();
        if (firstRelevantRank.isEmpty()) {
            return 0.0;
        }
        return 1.0 / firstRelevantRank.getAsInt();
    }

    private ToolResult evaluateTools(GoldenTestCase testCase, AgentExecution execution) {
        ToolExpectations expectations = new ToolExpectations(
                new LinkedHashSet<>(testCase.expected().requiredTools()),
                new LinkedHashSet<>(testCase.expected().forbiddenTools()));
        if (expectations.isEmpty()) {
            return noToolEvaluation();
        }

        Set<String> calledToolNames = extractCalledToolNames(execution);
        ToolCallClassification classification = classifyToolCalls(expectations, calledToolNames);
        return createToolResult(expectations, classification);
    }

    private Set<String> extractCalledToolNames(AgentExecution execution) {
        return execution.toolCalls().stream()
                .map(ToolCall::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ToolCallClassification classifyToolCalls(
            ToolExpectations expectations,
            Set<String> calledToolNames) {
        List<String> calledRequiredTools = expectations.requiredToolNames().stream()
                .filter(calledToolNames::contains)
                .toList();
        List<String> missingRequiredTools = expectations.requiredToolNames().stream()
                .filter(toolName -> !calledToolNames.contains(toolName))
                .toList();
        List<String> calledForbiddenTools = expectations.forbiddenToolNames().stream()
                .filter(calledToolNames::contains)
                .toList();
        List<String> unexpectedTools = calledToolNames.stream()
                .filter(toolName -> !expectations.requiredToolNames().contains(toolName))
                .filter(toolName -> !expectations.forbiddenToolNames().contains(toolName))
                .toList();
        return new ToolCallClassification(
                calledRequiredTools,
                missingRequiredTools,
                unexpectedTools,
                calledForbiddenTools);
    }

    private ToolResult createToolResult(
            ToolExpectations expectations,
            ToolCallClassification classification) {
        int satisfiedChecks = classification.calledRequiredTools().size()
                + expectations.forbiddenToolNames().size()
                - classification.calledForbiddenTools().size();
        int totalChecks = expectations.requiredToolNames().size()
                + expectations.forbiddenToolNames().size()
                + classification.unexpectedTools().size();
        return new ToolResult(
                MetricValue.of(calculateAccuracy(satisfiedChecks, totalChecks)),
                classification.calledRequiredTools(),
                classification.missingRequiredTools(),
                classification.unexpectedTools(),
                classification.calledForbiddenTools());
    }

    private ToolResult noToolEvaluation() {
        return new ToolResult(MetricValue.notApplicable(), List.of(), List.of(), List.of(), List.of());
    }

    private double calculateAccuracy(int satisfiedChecks, int totalChecks) {
        return (double) satisfiedChecks / totalChecks;
    }

    private List<PolicyViolation> createPolicyViolations(ToolResult toolResult) {
        return toolResult.forbiddenCalled().stream()
                .map(toolName -> new PolicyViolation(
                        PolicyViolationType.FORBIDDEN_TOOL_CALL,
                        toolName,
                        "Forbidden tool was called: " + toolName))
                .toList();
    }

    private List<String> createFailureReasons(
            RetrievalResult retrievalResult,
            ToolResult toolResult,
            int retrievalCutoff) {
        List<String> failureReasons = new ArrayList<>();
        addRetrievalFailure(failureReasons, retrievalResult, retrievalCutoff);
        toolResult.missingRequired().forEach(
                toolName -> failureReasons.add("Missing required tool: " + toolName));
        toolResult.unexpectedCalled().forEach(
                toolName -> failureReasons.add("Unexpected tool: " + toolName));
        toolResult.forbiddenCalled().forEach(
                toolName -> failureReasons.add("Forbidden tool: " + toolName));
        return List.copyOf(failureReasons);
    }

    private void addRetrievalFailure(
            List<String> failureReasons,
            RetrievalResult retrievalResult,
            int retrievalCutoff) {
        MetricValue recall = retrievalResult.recallAtK();
        if (recall.applicable() && recall.value() < 1.0) {
            failureReasons.add("Not all relevant documents were retrieved in the top " + retrievalCutoff);
        }
    }

    private EvaluationStatus statusFor(List<String> failureReasons) {
        return failureReasons.isEmpty() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
    }

    private record ToolExpectations(
            Set<String> requiredToolNames,
            Set<String> forbiddenToolNames) {

        private boolean isEmpty() {
            return requiredToolNames.isEmpty() && forbiddenToolNames.isEmpty();
        }
    }

    private record ToolCallClassification(
            List<String> calledRequiredTools,
            List<String> missingRequiredTools,
            List<String> unexpectedTools,
            List<String> calledForbiddenTools) {
    }
}
