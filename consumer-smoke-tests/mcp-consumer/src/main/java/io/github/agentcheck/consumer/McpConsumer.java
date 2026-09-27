package io.github.agentcheck.consumer;

import io.github.agentcheck.integration.mcp.McpExecutionMapper;
import io.github.agentcheck.model.ToolCall;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.List;
import java.util.Map;

public final class McpConsumer {
    private McpConsumer() {
    }

    public static void main(String[] arguments) {
        CallToolRequest request = CallToolRequest.builder("get_order")
                .arguments(Map.of("orderId", 42))
                .build();

        List<ToolCall> toolCalls = new McpExecutionMapper().mapToolCalls(List.of(request));
        if (!toolCalls.equals(List.of(new ToolCall("get_order", Map.of("orderId", 42))))) {
            throw new IllegalStateException("MCP consumer mapping failed");
        }
    }
}
