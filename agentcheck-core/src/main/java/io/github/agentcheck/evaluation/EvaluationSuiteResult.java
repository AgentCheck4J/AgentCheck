package io.github.agentcheck.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Aggregated result for a complete golden test suite.
 *
 * @param suite suite name
 * @param cases number of evaluated cases
 * @param passed number of passing cases
 * @param failed number of failing cases
 * @param skipped number of disabled cases
 * @param retrieval aggregated retrieval metrics
 * @param tools aggregated tool metrics
 * @param policy aggregated policy result
 * @param status overall suite status
 * @param failureReasons suite-level threshold failures
 * @param caseResults individual enabled-case results
 */
@JsonPropertyOrder({
        "suite", "cases", "passed", "failed", "skipped", "retrieval", "tools",
        "policy", "status", "failureReasons", "caseResults"
})
public record EvaluationSuiteResult(
        String suite,
        int cases,
        int passed,
        int failed,
        int skipped,
        RetrievalSummary retrieval,
        ToolSummary tools,
        PolicySummary policy,
        EvaluationStatus status,
        List<String> failureReasons,
        List<EvaluationCaseResult> caseResults) {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** Creates an immutable suite result. */
    public EvaluationSuiteResult {
        failureReasons = List.copyOf(failureReasons);
        caseResults = List.copyOf(caseResults);
    }

    /**
     * Creates a suite result without skipped cases.
     *
     * @param suite suite name
     * @param cases number of evaluated cases
     * @param passed number of passing cases
     * @param failed number of failing cases
     * @param retrieval aggregated retrieval metrics
     * @param tools aggregated tool metrics
     * @param policy aggregated policy result
     * @param status overall suite status
     * @param failureReasons suite-level threshold failures
     * @param caseResults individual case results
     */
    public EvaluationSuiteResult(
            String suite,
            int cases,
            int passed,
            int failed,
            RetrievalSummary retrieval,
            ToolSummary tools,
            PolicySummary policy,
            EvaluationStatus status,
            List<String> failureReasons,
            List<EvaluationCaseResult> caseResults) {
        this(suite, cases, passed, failed, 0, retrieval, tools, policy, status, failureReasons, caseResults);
    }

    /**
     * Reports whether all case-level and suite-level checks passed.
     *
     * @return {@code true} when the suite status is {@link EvaluationStatus#PASS}
     */
    @JsonIgnore
    public boolean passedAllChecks() {
        return status == EvaluationStatus.PASS;
    }

    /**
     * Serializes this result as stable, indented JSON.
     *
     * @return JSON representation
     */
    public String toJson() {
        try {
            return JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize evaluation result", exception);
        }
    }

    /**
     * Loads an evaluation result from JSON.
     *
     * @param path JSON file
     * @return deserialized evaluation result
     */
    public static EvaluationSuiteResult loadJson(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return JSON_MAPPER.readValue(input, EvaluationSuiteResult.class);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Could not load evaluation result '" + path + "': " + exception.getMessage(),
                    exception);
        }
    }
}
