package io.github.agentcheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.golden.Thresholds;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuiteParsingAndSerializationTest {
    @TempDir Path tempDir;

    @Test
    void parsesValidYamlAndMissingOptionalSections() throws IOException {
        var path = write("""
                suite: simple
                cases:
                  - id: answer-only
                    input: Hello
                  - id: retrieval
                    input: Find it
                    expected:
                      retrieval:
                        relevantDocuments: [doc.md]
                """);
        var suite = GoldenTestSuite.load(path);
        assertThat(suite.suite()).isEqualTo("simple");
        assertThat(suite.retrievalK()).isEqualTo(5);
        assertThat(suite.cases()).hasSize(2);
        assertThat(suite.cases().getFirst().expected()).isEqualTo(ExpectedBehaviour.none());
    }

    @Test
    void malformedYamlAndValidationErrorsAreUseful() throws IOException {
        assertThatThrownBy(() -> GoldenTestSuite.load(write("suite: [broken")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not load golden suite");
        assertThatThrownBy(() -> GoldenTestSuite.load(write("suite: ok\ncases:\n  - input: hi\n")))
                .hasMessageContaining("cases[0].id");
    }

    @Test
    void allCasesAndThresholdsCanPass() {
        var suite = suite(new Thresholds(1.0, 1.0, null, 0));
        var result = new AgentCheck().evaluate(suite, executions(true));
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
        assertThat(result.passed()).isEqualTo(2);
    }

    @Test
    void oneCaseFailureAndThresholdFailureAreVisible() {
        var suite = suite(new Thresholds(0.8, 0.8, null, 0));
        var result = new AgentCheck().evaluate(suite, executions(false));
        assertThat(result.status()).isEqualTo(EvaluationStatus.FAIL);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.failureReasons()).anyMatch(reason -> reason.contains("Recall@5"));
    }

    @Test
    void nonApplicableMetricDoesNotBecomeZeroOrFailThreshold() {
        var testCase = new GoldenTestCase("plain", "plain", ExpectedBehaviour.none());
        var suite = new GoldenTestSuite("plain", 5, List.of(testCase), new Thresholds(1.0, 1.0, 1.0, 0));
        var result = new AgentCheck().evaluate(suite, Map.of("plain", AgentExecution.of("plain", List.of(), List.of())));
        assertThat(result.retrieval().mrr().applicable()).isFalse();
        assertThat(result.tools().accuracy().applicable()).isFalse();
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void jsonHasStableReadableTopLevelFields() throws Exception {
        var result = new AgentCheck().evaluate(suite(Thresholds.defaults()), executions(true));
        JsonNode json = new ObjectMapper().readTree(result.toJson());
        assertThat(json.path("suite").asText()).isEqualTo("aggregate");
        assertThat(json.path("cases").asInt()).isEqualTo(2);
        assertThat(json.path("retrieval").path("recallAtK").path("value").asDouble()).isEqualTo(1.0);
        assertThat(json.path("policy").path("violations").asInt()).isZero();
        assertThat(json.path("status").asText()).isEqualTo("PASS");
    }

    private GoldenTestSuite suite(Thresholds thresholds) {
        var one = new GoldenTestCase("one", "one", new ExpectedBehaviour(List.of("a"), List.of(), List.of()));
        var two = new GoldenTestCase("two", "two", new ExpectedBehaviour(List.of("b"), List.of(), List.of()));
        return new GoldenTestSuite("aggregate", 5, List.of(one, two), thresholds);
    }

    private Map<String, AgentExecution> executions(boolean secondPasses) {
        return Map.of(
                "one", AgentExecution.of("one", List.of(new RetrievedDocument("a", 1)), List.of()),
                "two", AgentExecution.of("two", secondPasses ? List.of(new RetrievedDocument("b", 1)) : List.of(), List.of()));
    }

    private Path write(String yaml) throws IOException {
        var path = tempDir.resolve("suite.yaml");
        Files.writeString(path, yaml);
        return path;
    }
}
