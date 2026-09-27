package io.github.agentcheck.integration.mcp;

import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpExecutionMapperTest {
    private final McpExecutionMapper mapper = new McpExecutionMapper();

    @Test
    void mapsToolNamesArgumentsAndInvocationOrder() {
        LinkedHashMap<String, Object> firstArguments = new LinkedHashMap<>();
        firstArguments.put("orderId", 42);
        firstArguments.put("note", null);
        CallToolRequest firstRequest = CallToolRequest.builder("get_order")
                .arguments(firstArguments)
                .build();
        CallToolRequest secondRequest = CallToolRequest.builder("get_shipping_status").build();

        List<ToolCall> toolCalls = mapper.mapToolCalls(List.of(firstRequest, secondRequest));

        assertThat(toolCalls).extracting(ToolCall::name)
                .containsExactly("get_order", "get_shipping_status");
        assertThat(toolCalls.getFirst().arguments())
                .containsEntry("orderId", 42)
                .containsEntry("note", null);
        assertThat(toolCalls.get(1).arguments()).isEmpty();
    }

    @Test
    void combinesCallerOwnedAgentDataWithMcpToolCalls() {
        RetrievedDocument document = new RetrievedDocument("shipping-policy.md", 1, 0.91);
        CallToolRequest request = CallToolRequest.builder("get_order")
                .arguments(Map.of("orderId", 42))
                .build();

        AgentExecution execution = mapper.map(
                "Where is my order?",
                "Your order is in transit.",
                List.of(document),
                List.of(request));

        assertThat(execution.input()).isEqualTo("Where is my order?");
        assertThat(execution.answer()).isEqualTo("Your order is in transit.");
        assertThat(execution.retrievedDocuments()).containsExactly(document);
        assertThat(execution.toolCalls()).containsExactly(new ToolCall("get_order", Map.of("orderId", 42)));
    }

    @Test
    void rejectsMissingToolRequestList() {
        assertThatThrownBy(() -> mapper.mapToolCalls(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MCP tool requests must not be null");
    }

    @Test
    void identifiesNullToolRequestByIndex() {
        CallToolRequest request = CallToolRequest.builder("get_order").build();
        List<CallToolRequest> requests = Arrays.asList(request, null);

        assertThatThrownBy(() -> mapper.mapToolCalls(requests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MCP tool request at index 1 must not be null");
    }
}
