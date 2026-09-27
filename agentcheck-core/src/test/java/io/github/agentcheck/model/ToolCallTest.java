package io.github.agentcheck.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCallTest {
    @Test
    void preservesJsonNullArgumentsAndInsertionOrder() {
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("orderId", 42);
        arguments.put("note", null);

        ToolCall toolCall = new ToolCall("get_order", arguments);

        assertThat(toolCall.arguments()).containsExactly(
                Map.entry("orderId", 42),
                new java.util.AbstractMap.SimpleImmutableEntry<>("note", null));
    }

    @Test
    void defensivelyCopiesArguments() {
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("orderId", 42);

        ToolCall toolCall = new ToolCall("get_order", arguments);
        arguments.put("includeHistory", true);

        assertThat(toolCall.arguments()).containsOnly(Map.entry("orderId", 42));
        assertThatThrownBy(() -> toolCall.arguments().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullArgumentNames() {
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put(null, "value");

        assertThatThrownBy(() -> new ToolCall("get_order", arguments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tool argument name must not be null");
    }
}
