package io.github.agentcheck.example;

import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;

import java.util.List;

public final class FakeSupportAgent implements AgentAdapter {
    @Override
    public AgentExecution execute(String input) {
        if (input.contains("Where is my order")) {
            return new AgentExecution(input, "Your order is in transit.",
                    List.of(new RetrievedDocument("shipping-policy.md", 1)),
                    List.of(new ToolCall("get_order"), new ToolCall("get_shipping_status")));
        }
        if (input.contains("Refund me")) {
            return new AgentExecution(input, "I found the order and escalated your request.",
                    List.of(
                            new RetrievedDocument("shipping-policy.md", 1),
                            new RetrievedDocument("refund-policy.md", 2)),
                    List.of(new ToolCall("get_order")));
        }
        return new AgentExecution(input, "I cannot help with that request.", List.of(), List.of());
    }
}
