package io.github.agentcheck;

import io.github.agentcheck.model.AgentExecution;

/** Executes an agent for one golden-test input and exposes its observable behaviour. */
@FunctionalInterface
public interface AgentAdapter {
    /**
     * Executes the agent once.
     *
     * @param input input from the golden test case
     * @return the observed answer, retrieval results, and tool calls
     */
    AgentExecution execute(String input);
}
