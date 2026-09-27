package io.github.agentcheck.policy;

/**
 * One deterministic policy violation found during evaluation.
 *
 * @param type violation category
 * @param tool tool responsible for the violation
 * @param message human-readable explanation
 */
public record PolicyViolation(PolicyViolationType type, String tool, String message) { }
