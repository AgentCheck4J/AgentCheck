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

    public EvaluationSuiteResult {
        failureReasons = List.copyOf(failureReasons);
        caseResults = List.copyOf(caseResults);
    }

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

    @JsonIgnore
    public boolean passedAllChecks() {
        return status == EvaluationStatus.PASS;
    }

    public String toJson() {
        try {
            return JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize evaluation result", exception);
        }
    }

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
