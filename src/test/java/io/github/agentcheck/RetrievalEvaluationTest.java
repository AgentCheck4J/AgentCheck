package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.golden.GoldenTestCase;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.golden.Thresholds;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalEvaluationTest {
    private final AgentCheck evaluator = new AgentCheck();

    @Test
    void computesRecallAtKAndReciprocalRankForMultipleRelevantDocuments() {
        var result = evaluator.evaluate(testCase(List.of("a", "c")), execution(
                new RetrievedDocument("x", 1), new RetrievedDocument("a", 2), new RetrievedDocument("c", 7)));

        assertThat(result.retrieval().recallAtK().value()).isEqualTo(0.5);
        assertThat(result.retrieval().reciprocalRank().value()).isEqualTo(0.5);
        assertThat(result.status()).isEqualTo(EvaluationStatus.FAIL);
    }

    @Test
    void rankOneRelevantDocumentGetsPerfectReciprocalRank() {
        var result = evaluator.evaluate(testCase(List.of("a")), execution(new RetrievedDocument("a", 1)));
        assertThat(result.retrieval().reciprocalRank().value()).isEqualTo(1.0);
    }

    @Test
    void relevantDocumentOutsideKHasZeroRecallButNonzeroReciprocalRank() {
        var documents = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(rank -> new RetrievedDocument(rank == 6 ? "relevant" : "x" + rank, rank)).toList();
        var result = evaluator.evaluate(testCase(List.of("relevant")), AgentExecution.of("input", documents, List.of()));

        assertThat(result.retrieval().recallAtK().value()).isZero();
        assertThat(result.retrieval().reciprocalRank().value()).isEqualTo(1.0 / 6.0);
    }

    @Test
    void emptyRetrievalProducesZeroApplicableMetrics() {
        var result = evaluator.evaluate(testCase(List.of("a")), execution());
        assertThat(result.retrieval().recallAtK().value()).isZero();
        assertThat(result.retrieval().reciprocalRank().value()).isZero();
        assertThat(result.retrieval().recallAtK().applicable()).isTrue();
    }

    @Test
    void duplicateDocumentIdsAreIgnoredWithoutRewritingDeclaredRanks() {
        var result = evaluator.evaluate(testCase(List.of("a")), execution(
                new RetrievedDocument("x", 1), new RetrievedDocument("x", 2),
                new RetrievedDocument("y", 3), new RetrievedDocument("z", 4),
                new RetrievedDocument("q", 5), new RetrievedDocument("a", 6)));

        assertThat(result.retrieval().recallAtK().value()).isZero();
        assertThat(result.retrieval().reciprocalRank().value()).isEqualTo(1.0 / 6.0);
    }

    @Test
    void retrievalWithoutRelevanceJudgmentsIsNotApplicable() {
        var result = evaluator.evaluate(testCase(List.of()), execution(new RetrievedDocument("a", 1)));
        assertThat(result.retrieval().recallAtK().applicable()).isFalse();
        assertThat(result.retrieval().recallAtK().value()).isNull();
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
    }

    @Test
    void suiteMrrAveragesOnlyApplicableCases() {
        var one = testCase("one", List.of("a"));
        var two = testCase("two", List.of("b"));
        var noRetrieval = testCase("none", List.of());
        var suite = new GoldenTestSuite("suite", 5, List.of(one, two, noRetrieval), Thresholds.defaults());
        var result = evaluator.evaluate(suite, Map.of(
                "one", AgentExecution.of("input", List.of(new RetrievedDocument("a", 1)), List.of()),
                "two", AgentExecution.of("input", List.of(new RetrievedDocument("x", 1), new RetrievedDocument("b", 2)), List.of()),
                "none", AgentExecution.of("input", List.of(), List.of())));

        assertThat(result.retrieval().mrr().value()).isEqualTo(0.75);
        assertThat(result.retrieval().recallAtK().value()).isEqualTo(1.0);
    }

    private GoldenTestCase testCase(List<String> relevant) { return testCase("case", relevant); }
    private GoldenTestCase testCase(String id, List<String> relevant) {
        return new GoldenTestCase(id, "input", new ExpectedBehaviour(relevant, List.of(), List.of()));
    }
    private AgentExecution execution(RetrievedDocument... documents) {
        return AgentExecution.of("input", List.of(documents), List.of());
    }
}
