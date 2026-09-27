package io.github.agentcheck.consumer;

import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.integration.springai.SpringAiAgentAdapter;
import org.springframework.ai.chat.client.ChatClient;

public final class SpringAiConsumer {
    private SpringAiConsumer() {
    }

    public static void main(String[] arguments) {
        Class<ChatClient> chatClientType = ChatClient.class;
        if (!AgentAdapter.class.isAssignableFrom(SpringAiAgentAdapter.class)) {
            throw new IllegalStateException("Spring AI adapter does not implement AgentAdapter");
        }
        if (!chatClientType.getName().equals("org.springframework.ai.chat.client.ChatClient")) {
            throw new IllegalStateException("Spring AI client dependency is unavailable");
        }
    }
}
