package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.ToolCall;
import io.github.agentcheck.policy.PolicyViolationType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolAndPolicyEvaluationTest {
    private final AgentCheck agentCheck = new AgentCheck();

    @Test
    void allRequiredToolsAndNoForbiddenCallsPass() {
        EvaluationCaseResult result = evaluate(
                List.of("get_order", "status"),
                List.of("cancel"),
                "get_order",
                "status");
        assertThat(result.tools().accuracy().value()).isEqualTo(1.0);
        assertThat(result.tools().missingRequired()).isEmpty();
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void reportsMissingUnexpectedAndForbiddenToolsSeparately() {
        EvaluationCaseResult result = evaluate(
                List.of("get_order", "status"),
                List.of("cancel"),
                "get_order",
                "cancel",
                "weather");
        assertThat(result.tools().calledRequired()).containsExactly("get_order");
        assertThat(result.tools().missingRequired()).containsExactly("status");
        assertThat(result.tools().unexpectedCalled()).containsExactly("weather");
        assertThat(result.tools().forbiddenCalled()).containsExactly("cancel");
        assertThat(result.policyViolations()).singleElement().satisfies(violation ->
                assertThat(violation.type()).isEqualTo(PolicyViolationType.FORBIDDEN_TOOL_CALL));
        assertThat(result.status()).isEqualTo(EvaluationStatus.FAIL);
    }

    @Test
    void duplicateCallsCountOnce() {
        EvaluationCaseResult result = evaluate(List.of("get_order"), List.of(), "get_order", "get_order");
        assertThat(result.tools().calledRequired()).containsExactly("get_order");
        assertThat(result.tools().accuracy().value()).isEqualTo(1.0);
    }

    @Test
    void noToolExpectationsIsNotApplicableAndDoesNotJudgeCalls() {
        EvaluationCaseResult result = evaluate(List.of(), List.of(), "anything");
        assertThat(result.tools().accuracy().applicable()).isFalse();
        assertThat(result.tools().unexpectedCalled()).isEmpty();
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void createsOneViolationPerDistinctForbiddenTool() {
        EvaluationCaseResult result = evaluate(List.of(), List.of("cancel", "delete"), "cancel", "delete", "delete");
        assertThat(result.policyViolations()).hasSize(2);
    }

    private EvaluationCaseResult evaluate(
            List<String> requiredTools,
            List<String> forbiddenTools,
            String... calledTools) {
        ExpectedBehaviour expectedBehaviour = new ExpectedBehaviour(
                List.of(),
                requiredTools,
                forbiddenTools);
        GoldenTestCase testCase = new GoldenTestCase("case", "input", expectedBehaviour);
        List<ToolCall> toolCalls = Arrays.stream(calledTools).map(ToolCall::new).toList();
        return agentCheck.evaluate(testCase, AgentExecution.of("input", List.of(), toolCalls));
    }
}
