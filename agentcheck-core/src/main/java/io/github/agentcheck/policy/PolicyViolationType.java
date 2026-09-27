package io.github.agentcheck.policy;

/** Categories of deterministic policy violations. */
public enum PolicyViolationType {
    /** A tool explicitly forbidden by the golden case was called. */
    FORBIDDEN_TOOL_CALL
}
