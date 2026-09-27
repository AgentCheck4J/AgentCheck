package io.github.agentcheck.model;

import java.util.List;

/**
 * Immutable observed output from one agent execution.
 *
 * @param input input sent to the agent
 * @param answer final textual answer
 * @param retrievedDocuments retrieved documents
 * @param toolCalls tool calls in observed order
 */
public record AgentExecution(
        String input,
        String answer,
        List<RetrievedDocument> retrievedDocuments,
        List<ToolCall> toolCalls) {

    /** Validates and defensively copies the observed execution data. */
    public AgentExecution {
        if (input == null) {
            throw new IllegalArgumentException("execution input must not be null");
        }
        answer = answer == null ? "" : answer;
        retrievedDocuments = retrievedDocuments == null ? List.of() : List.copyOf(retrievedDocuments);
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    /**
     * Creates an execution without a textual answer.
     *
     * @param input input sent to the agent
     * @param documents retrieved documents
     * @param tools observed tool calls
     * @return immutable execution
     */
    public static AgentExecution of(String input, List<RetrievedDocument> documents, List<ToolCall> tools) {
        return new AgentExecution(input, "", documents, tools);
    }
}
