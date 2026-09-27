package io.github.agentcheck;

import io.github.agentcheck.model.AgentExecution;

@FunctionalInterface
public interface AgentAdapter {
    AgentExecution execute(String input);
}
