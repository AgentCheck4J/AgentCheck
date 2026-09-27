package io.github.agentcheck.regression;

public record CountComparison(
        String metric,
        int baseline,
        int current,
        int change,
        boolean regression) { }
