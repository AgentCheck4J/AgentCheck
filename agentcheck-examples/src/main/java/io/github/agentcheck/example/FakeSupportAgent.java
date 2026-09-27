package io.github.agentcheck.example;

import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;

import java.util.List;

/** Deterministic fake agent used by the customer-support example. */
public final class FakeSupportAgent implements AgentAdapter {
    /** Creates the fake support agent. */
    public FakeSupportAgent() {
    }

    @Override
    public AgentExecution execute(String input) {
        if (isOrderStatusQuestion(input)) {
            return orderStatusExecution(input);
        }
        if (isRefundRequest(input)) {
            return refundExecution(input);
        }
        return new AgentExecution(input, "I cannot help with that request.", List.of(), List.of());
    }

    private boolean isOrderStatusQuestion(String input) {
        return input.contains("Where is my order");
    }

    private AgentExecution orderStatusExecution(String input) {
        return new AgentExecution(
                input,
                "Your order is in transit.",
                List.of(new RetrievedDocument("shipping-policy.md", 1)),
                List.of(new ToolCall("get_order"), new ToolCall("get_shipping_status")));
    }

    private boolean isRefundRequest(String input) {
        return input.contains("Refund me");
    }

    private AgentExecution refundExecution(String input) {
        return new AgentExecution(
                input,
                "I found the order and escalated your request.",
                List.of(
                        new RetrievedDocument("shipping-policy.md", 1),
                        new RetrievedDocument("refund-policy.md", 2)),
                List.of(new ToolCall("get_order")));
    }
}
