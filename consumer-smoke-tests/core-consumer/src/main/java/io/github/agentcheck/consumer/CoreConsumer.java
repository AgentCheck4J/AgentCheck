package io.github.agentcheck.consumer;

import io.github.agentcheck.AgentCheck;
import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.ToolCall;

import java.util.List;

public final class CoreConsumer {
    private CoreConsumer() {
    }

    public static void main(String[] arguments) {
        GoldenTestCase testCase = new GoldenTestCase(
                "core-consumer",
                "Check my order",
                new ExpectedBehaviour(List.of(), List.of("get_order"), List.of()));
        AgentExecution execution = AgentExecution.of(
                "Check my order",
                List.of(),
                List.of(new ToolCall("get_order")));

        EvaluationCaseResult result = new AgentCheck().evaluate(testCase, execution);
        if (result.status() != EvaluationStatus.PASS) {
            throw new IllegalStateException("core consumer evaluation failed");
        }

        requireClassAbsent("org.springframework.ai.chat.client.ChatClient");
        requireClassAbsent("io.modelcontextprotocol.spec.McpSchema");
    }

    private static void requireClassAbsent(String className) {
        try {
            Class.forName(className);
            throw new IllegalStateException("core consumer unexpectedly contains " + className);
        } catch (ClassNotFoundException expected) {
            // Expected: core consumers must not receive optional integration SDKs.
        }
    }
}
