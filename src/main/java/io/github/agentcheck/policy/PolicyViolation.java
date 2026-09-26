package io.github.agentcheck.policy;

public record PolicyViolation(PolicyViolationType type, String tool, String message) { }
