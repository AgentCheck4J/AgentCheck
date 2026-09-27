package io.github.agentcheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.golden.Thresholds;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import io.github.agentcheck.report.ConsoleReporter;
import io.github.agentcheck.regression.RegressionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegressionComparisonTest {
    private final AgentCheck agentCheck = new AgentCheck();
    @TempDir Path tempDir;

    @Test
    void detectsMetricCountAndPolicyRegressionsWithoutCompositeScore() {
        RegressionResult comparison = agentCheck.compare(goodResult(), regressedResult());

        assertThat(comparison.status()).isEqualTo(EvaluationStatus.FAIL);
        assertThat(comparison.metrics()).filteredOn(metric -> metric.regression())
                .extracting(metric -> metric.metric())
                .containsExactly("Recall@5", "MRR", "Tool accuracy");
        assertThat(comparison.counts()).filteredOn(count -> count.regression())
                .extracting(count -> count.metric())
                .containsExactly("Failed cases", "Missing tools", "Unexpected tools", "Policy violations");
        assertThat(comparison.regressionReasons()).anyMatch(reason -> reason.contains("decreased"));
    }

    @Test
    void improvementsPassRegressionComparison() {
        RegressionResult comparison = agentCheck.compare(regressedResult(), goodResult());
        assertThat(comparison.status()).isEqualTo(EvaluationStatus.PASS);
        assertThat(comparison.regressionReasons()).isEmpty();
    }

    @Test
    void applicableMetricBecomingNotApplicableIsARegression() {
        EvaluationSuiteResult baseline = retrievalOnlyResult(true);
        EvaluationSuiteResult current = retrievalOnlyResult(false);
        RegressionResult comparison = agentCheck.compare(baseline, current);

        assertThat(comparison.metrics().getFirst().regression()).isTrue();
        assertThat(comparison.regressionReasons()).anyMatch(reason -> reason.contains("became not applicable"));
    }

    @Test
    void newApplicableMetricIsNotARegression() {
        RegressionResult comparison = agentCheck.compare(retrievalOnlyResult(false), retrievalOnlyResult(true));
        assertThat(comparison.metrics()).noneMatch(metric -> metric.regression());
    }

    @Test
    void rejectsDifferentSuitesCutoffsAndCaseIds() {
        EvaluationSuiteResult good = goodResult();
        EvaluationSuiteResult otherName = result("other", "case", 5, true);
        EvaluationSuiteResult otherK = result("regression", "case", 3, true);
        EvaluationSuiteResult otherCase = result("regression", "other-case", 5, true);

        assertThatThrownBy(() -> agentCheck.compare(good, otherName)).hasMessageContaining("different suites");
        assertThatThrownBy(() -> agentCheck.compare(good, otherK)).hasMessageContaining("different retrieval cutoffs");
        assertThatThrownBy(() -> agentCheck.compare(good, otherCase)).hasMessageContaining("different case IDs");
    }

    @Test
    void evaluationJsonCanBeSavedLoadedAndCompared() throws Exception {
        Path path = tempDir.resolve("baseline.json");
        Files.writeString(path, goodResult().toJson());

        EvaluationSuiteResult loaded = EvaluationSuiteResult.loadJson(path);
        RegressionResult comparison = agentCheck.compare(loaded, goodResult());

        assertThat(loaded.suite()).isEqualTo("regression");
        assertThat(comparison.status()).isEqualTo(EvaluationStatus.PASS);
        assertThat(comparison.toJson()).contains("\"relativeChange\"");
    }

    @Test
    void loadsV01JsonWithoutSkippedField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) mapper.readTree(goodResult().toJson());
        json.remove("skipped");
        Path path = tempDir.resolve("v0.1-baseline.json");
        mapper.writeValue(path.toFile(), json);

        EvaluationSuiteResult loaded = EvaluationSuiteResult.loadJson(path);

        assertThat(loaded.skipped()).isZero();
        assertThat(agentCheck.compare(loaded, goodResult()).status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void consoleReporterShowsBaselineCurrentAndReasons() {
        String output = new ConsoleReporter().render(agentCheck.compare(goodResult(), regressedResult()));
        assertThat(output).contains("AgentCheck Regression", "Baseline", "Current", "Policy violations", "Result                 FAIL");
    }

    private EvaluationSuiteResult goodResult() {
        return result("regression", "case", 5, true);
    }

    private EvaluationSuiteResult regressedResult() {
        return result("regression", "case", 5, false);
    }

    private EvaluationSuiteResult result(String suiteName, String caseId, int k, boolean good) {
        GoldenTestCase testCase = new GoldenTestCase(caseId, "input",
                new ExpectedBehaviour(List.of("doc"), List.of("required"), List.of("forbidden")));
        GoldenTestSuite suite = new GoldenTestSuite(suiteName, k, List.of(testCase), Thresholds.defaults());
        AgentExecution execution = new AgentExecution(
                "input", "answer",
                List.of(new RetrievedDocument("doc", good ? 1 : k + 1)),
                good ? List.of(new ToolCall("required")) : List.of(new ToolCall("forbidden"), new ToolCall("extra")));
        return agentCheck.evaluate(suite, Map.of(caseId, execution));
    }

    private EvaluationSuiteResult retrievalOnlyResult(boolean applicable) {
        ExpectedBehaviour expected = applicable
                ? new ExpectedBehaviour(List.of("doc"), List.of(), List.of())
                : ExpectedBehaviour.none();
        GoldenTestCase testCase = new GoldenTestCase("case", "input", expected);
        GoldenTestSuite suite = new GoldenTestSuite("applicability", 5, List.of(testCase), Thresholds.defaults());
        return agentCheck.evaluate(suite, Map.of("case", AgentExecution.of(
                "input", applicable ? List.of(new RetrievedDocument("doc", 1)) : List.of(), List.of())));
    }
}
