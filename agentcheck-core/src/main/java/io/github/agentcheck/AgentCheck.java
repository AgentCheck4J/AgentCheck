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

/** Main facade for evaluating agent executions and comparing evaluation runs. */
public final class AgentCheck {
    private static final int DEFAULT_RETRIEVAL_CUTOFF = 5;

    private final CaseEvaluator caseEvaluator = new CaseEvaluator();
    private final SuiteResultAggregator suiteResultAggregator = new SuiteResultAggregator();
    private final RegressionComparator regressionComparator = new RegressionComparator();

    /** Creates an evaluator with the built-in deterministic checks. */
    public AgentCheck() {
    }

    /**
     * Evaluates one execution using the default retrieval cutoff.
     *
     * @param testCase expected behaviour
     * @param execution observed agent behaviour
     * @return the case-level evaluation result
     */
    public EvaluationCaseResult evaluate(GoldenTestCase testCase, AgentExecution execution) {
        return caseEvaluator.evaluate(testCase, execution, DEFAULT_RETRIEVAL_CUTOFF);
    }

    /**
     * Executes and evaluates all enabled cases in a suite.
     *
     * @param agent adapter used to execute each enabled case
     * @param suite golden test suite
     * @return the aggregated suite result
     */
    public EvaluationSuiteResult evaluate(AgentAdapter agent, GoldenTestSuite suite) {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(suite, "suite");

        Map<String, AgentExecution> executionsByCaseId = executeEnabledCases(agent, suite.cases());
        return evaluate(suite, executionsByCaseId);
    }

    /**
     * Evaluates a suite from previously captured executions keyed by case ID.
     *
     * @param suite golden test suite
     * @param executionsByCaseId executions for all enabled cases
     * @return the aggregated suite result
     */
    public EvaluationSuiteResult evaluate(
            GoldenTestSuite suite,
            Map<String, AgentExecution> executionsByCaseId) {
        Objects.requireNonNull(suite, "suite");
        Objects.requireNonNull(executionsByCaseId, "executionsByCaseId");

        List<EvaluationCaseResult> caseResults = evaluateEnabledCases(suite, executionsByCaseId);
        return suiteResultAggregator.aggregate(suite, caseResults);
    }

    /**
     * Compares a current suite result with its baseline.
     *
     * @param baseline previous evaluation result
     * @param current current evaluation result
     * @return deterministic regression comparison
     */
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
