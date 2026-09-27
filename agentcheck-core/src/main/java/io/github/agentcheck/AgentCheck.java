package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationCaseResult;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.regression.RegressionResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AgentCheck {
    private static final int DEFAULT_RETRIEVAL_CUTOFF = 5;

    private final CaseEvaluator caseEvaluator = new CaseEvaluator();
    private final SuiteResultAggregator suiteResultAggregator = new SuiteResultAggregator();
    private final RegressionComparator regressionComparator = new RegressionComparator();

    public EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution) {
        return caseEvaluator.evaluate(testCase, execution, DEFAULT_RETRIEVAL_CUTOFF);
    }

    public EvaluationSuiteResult evaluate(AgentAdapter agent, GoldenTestSuite suite) {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(suite, "suite");

        Map<String, AgentExecution> executionsByCaseId = executeEnabledCases(agent, suite.cases());
        return evaluate(suite, executionsByCaseId);
    }

    public EvaluationSuiteResult evaluate(
            GoldenTestSuite suite,
            Map<String, AgentExecution> executionsByCaseId) {
        Objects.requireNonNull(suite, "suite");
        Objects.requireNonNull(executionsByCaseId, "executionsByCaseId");

        List<EvaluationCaseResult> caseResults = evaluateEnabledCases(suite, executionsByCaseId);
        return suiteResultAggregator.aggregate(suite, caseResults);
    }

    public RegressionResult compare(EvaluationSuiteResult baseline, EvaluationSuiteResult current) {
        return regressionComparator.compare(baseline, current);
    }

    private Map<String, AgentExecution> executeEnabledCases(
            AgentAdapter agent,
            List<GoldenTestCase> testCases) {
        Map<String, AgentExecution> executionsByCaseId = new LinkedHashMap<>();
        for (GoldenTestCase testCase : testCases) {
            if (testCase.enabled()) {
                executionsByCaseId.put(testCase.id(), agent.execute(testCase.input()));
            }
        }
        return executionsByCaseId;
    }

    private List<EvaluationCaseResult> evaluateEnabledCases(
            GoldenTestSuite suite,
            Map<String, AgentExecution> executionsByCaseId) {
        return suite.cases().stream()
                .filter(GoldenTestCase::enabled)
                .map(testCase -> evaluateCase(testCase, executionsByCaseId, suite.retrievalK()))
                .toList();
    }

    private EvaluationCaseResult evaluateCase(
            GoldenTestCase testCase,
            Map<String, AgentExecution> executionsByCaseId,
            int retrievalCutoff) {
        AgentExecution execution = executionsByCaseId.get(testCase.id());
        if (execution == null) {
            throw new IllegalArgumentException("missing execution for case: " + testCase.id());
        }
        return caseEvaluator.evaluate(testCase, execution, retrievalCutoff);
    }
}
