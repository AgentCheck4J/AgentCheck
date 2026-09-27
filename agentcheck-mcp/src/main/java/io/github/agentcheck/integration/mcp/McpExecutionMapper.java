package io.github.agentcheck.integration.mcp;

import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.ArrayList;
import java.util.List;

/** Maps observed MCP tool requests into AgentCheck execution data. */
public final class McpExecutionMapper {
    /**
     * Creates an execution from caller-owned agent output and observed MCP requests.
     *
     * @param input evaluated agent input
     * @param answer final answer produced by the caller's agent
     * @param retrievedDocuments documents observed by the caller's agent
     * @param toolRequests MCP tool requests in invocation order
     * @return an immutable AgentCheck execution
     */
    public AgentExecution map(
            String input,
            String answer,
            List<RetrievedDocument> retrievedDocuments,
            List<CallToolRequest> toolRequests) {
        return new AgentExecution(input, answer, retrievedDocuments, mapToolCalls(toolRequests));
    }

    /**
     * Maps MCP tool requests in invocation order.
     *
     * @param toolRequests MCP tool requests
     * @return immutable AgentCheck tool calls
     */
    public List<ToolCall> mapToolCalls(List<CallToolRequest> toolRequests) {
        if (toolRequests == null) {
            throw new IllegalArgumentException("MCP tool requests must not be null");
        }
        List<ToolCall> toolCalls = new ArrayList<>();
        for (int index = 0; index < toolRequests.size(); index++) {
            toolCalls.add(mapToolCall(toolRequests.get(index), index));
        }
        return List.copyOf(toolCalls);
    }

    private ToolCall mapToolCall(CallToolRequest toolRequest, int index) {
        if (toolRequest == null) {
            throw new IllegalArgumentException("MCP tool request at index " + index + " must not be null");
        }
        return new ToolCall(toolRequest.name(), toolRequest.arguments());
    }
}
