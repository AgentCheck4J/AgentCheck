package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.ToolCall;
import io.github.agentcheck.policy.PolicyViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolAndPolicyEvaluationTest {
    private final AgentCheck evaluator = new AgentCheck();

    @Test
    void allRequiredToolsAndNoForbiddenCallsPass() {
        var result = evaluate(List.of("get_order", "status"), List.of("cancel"), "get_order", "status");
        assertThat(result.tools().accuracy().value()).isEqualTo(1.0);
        assertThat(result.tools().missingRequired()).isEmpty();
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void reportsMissingUnexpectedAndForbiddenToolsSeparately() {
        var result = evaluate(List.of("get_order", "status"), List.of("cancel"), "get_order", "cancel", "weather");
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
        var result = evaluate(List.of("get_order"), List.of(), "get_order", "get_order");
        assertThat(result.tools().calledRequired()).containsExactly("get_order");
        assertThat(result.tools().accuracy().value()).isEqualTo(1.0);
    }

    @Test
    void noToolExpectationsIsNotApplicableAndDoesNotJudgeCalls() {
        var result = evaluate(List.of(), List.of(), "anything");
        assertThat(result.tools().accuracy().applicable()).isFalse();
        assertThat(result.tools().unexpectedCalled()).isEmpty();
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void createsOneViolationPerDistinctForbiddenTool() {
        var result = evaluate(List.of(), List.of("cancel", "delete"), "cancel", "delete", "delete");
        assertThat(result.policyViolations()).hasSize(2);
    }

    private io.github.agentcheck.evaluation.EvaluationCaseResult evaluate(
            List<String> required, List<String> forbidden, String... called) {
        var testCase = new GoldenTestCase("case", "input", new ExpectedBehaviour(List.of(), required, forbidden));
        var tools = java.util.Arrays.stream(called).map(ToolCall::new).toList();
        return evaluator.evaluate(testCase, AgentExecution.of("input", List.of(), tools));
    }
}
