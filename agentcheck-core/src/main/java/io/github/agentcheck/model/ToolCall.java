package io.github.agentcheck.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool invocation observed during an agent execution.
 *
 * @param name tool name
 * @param arguments immutable argument map
 */
public record ToolCall(String name, Map<String, Object> arguments) {
    /** Validates the name and defensively copies the arguments. */
    public ToolCall {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool name must not be blank");
        }
        arguments = immutableArguments(arguments);
    }

    /**
     * Creates a tool call without arguments.
     *
     * @param name tool name
     */
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
