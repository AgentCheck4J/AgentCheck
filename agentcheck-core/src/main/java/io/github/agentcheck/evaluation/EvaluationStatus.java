package io.github.agentcheck.evaluation;

/** Overall status of an evaluation or regression comparison. */
public enum EvaluationStatus {
    /** Every required check passed. */
    PASS,
    /** At least one required check failed. */
    FAIL
}
