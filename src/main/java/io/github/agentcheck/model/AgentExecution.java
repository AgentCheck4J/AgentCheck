package io.github.agentcheck.model;

import java.util.List;

public record AgentExecution(
        String input,
        String answer,
        List<RetrievedDocument> retrievedDocuments,
        List<ToolCall> toolCalls) {

    public AgentExecution {
        if (input == null) throw new IllegalArgumentException("execution input must not be null");
        answer = answer == null ? "" : answer;
        retrievedDocuments = retrievedDocuments == null ? List.of() : List.copyOf(retrievedDocuments);
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AgentExecution of(String input, List<RetrievedDocument> documents, List<ToolCall> tools) {
        return new AgentExecution(input, "", documents, tools);
    }
}
