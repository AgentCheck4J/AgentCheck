package io.github.agentcheck.evaluation;

import io.github.agentcheck.policy.PolicyViolation;

import java.util.List;

public record EvaluationCaseResult(
        String caseId,
        RetrievalResult retrieval,
        ToolResult tools,
        List<PolicyViolation> policyViolations,
        EvaluationStatus status,
        List<String> failureReasons) {

    public EvaluationCaseResult {
        policyViolations = List.copyOf(policyViolations);
        failureReasons = List.copyOf(failureReasons);
    }
}
