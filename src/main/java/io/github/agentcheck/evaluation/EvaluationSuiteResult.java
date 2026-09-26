package io.github.agentcheck.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

@JsonPropertyOrder({"suite", "cases", "passed", "failed", "retrieval", "tools", "policy", "status", "failureReasons", "caseResults"})
public record EvaluationSuiteResult(
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

    public EvaluationSuiteResult {
        failureReasons = List.copyOf(failureReasons);
        caseResults = List.copyOf(caseResults);
    }

    @JsonIgnore
    public boolean passedAllChecks() {
        return status == EvaluationStatus.PASS;
    }

    public String toJson() {
        try {
            return new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(this);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize evaluation result", exception);
        }
    }
}
