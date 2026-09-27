package io.github.agentcheck.example;

import io.github.agentcheck.AgentCheck;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.golden.Thresholds;
import io.github.agentcheck.integration.mcp.McpExecutionMapper;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.report.ConsoleReporter;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.List;
import java.util.Map;

/** Demonstrates deterministic evaluation of an observed MCP tool request. */
public final class McpMappingExample {
    private static final String CASE_ID = "mcp-order-status";
    private static final String INPUT = "Where is my order?";

    private McpMappingExample() {
    }

    public static void main(String[] arguments) {
        CallToolRequest toolRequest = CallToolRequest.builder("get_order")
                .arguments(Map.of("orderId", 42))
                .build();
        RetrievedDocument policyDocument = new RetrievedDocument("shipping-policy.md", 1);
        AgentExecution execution = new McpExecutionMapper().map(
                INPUT,
                "Your order is in transit.",
                List.of(policyDocument),
                List.of(toolRequest));

        GoldenTestSuite suite = createGoldenSuite();
        EvaluationSuiteResult result = new AgentCheck().evaluate(suite, Map.of(CASE_ID, execution));
        new ConsoleReporter().print(result, System.out);
    }

    private static GoldenTestSuite createGoldenSuite() {
        ExpectedBehaviour expectedBehaviour = new ExpectedBehaviour(
                List.of("shipping-policy.md"),
                List.of("get_order"),
                List.of("cancel_order"));
        GoldenTestCase testCase = new GoldenTestCase(CASE_ID, INPUT, expectedBehaviour);
        return new GoldenTestSuite("mcp-mapping", 5, List.of(testCase), Thresholds.defaults());
    }
}
