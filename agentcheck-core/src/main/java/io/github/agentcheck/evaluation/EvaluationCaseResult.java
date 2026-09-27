package io.github.agentcheck.evaluation;

import io.github.agentcheck.policy.PolicyViolation;

import java.util.List;

/**
 * Complete deterministic evaluation result for one golden test case.
 *
 * @param caseId evaluated case identifier
 * @param retrieval retrieval metrics for the case
 * @param tools tool-behaviour result for the case
 * @param policyViolations detected policy violations
 * @param status overall case status
 * @param failureReasons human-readable reasons for a failed case
 */
public record EvaluationCaseResult(
        String caseId,
        RetrievalResult retrieval,
        ToolResult tools,
        List<PolicyViolation> policyViolations,
        EvaluationStatus status,
        List<String> failureReasons) {

    /** Creates an immutable case result. */
    public EvaluationCaseResult {
        policyViolations = List.copyOf(policyViolations);
        failureReasons = List.copyOf(failureReasons);
    }
}
