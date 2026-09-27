package io.github.agentcheck.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ToolCall(String name, Map<String, Object> arguments) {
    public ToolCall {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool name must not be blank");
        }
        arguments = immutableArguments(arguments);
    }

    public ToolCall(String name) {
        this(name, Map.of());
    }

    private static Map<String, Object> immutableArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> argumentsCopy = new LinkedHashMap<>();
        arguments.forEach((argumentName, argumentValue) -> {
            if (argumentName == null) {
                throw new IllegalArgumentException("tool argument name must not be null");
            }
            argumentsCopy.put(argumentName, argumentValue);
        });
        return Collections.unmodifiableMap(argumentsCopy);
    }
}
