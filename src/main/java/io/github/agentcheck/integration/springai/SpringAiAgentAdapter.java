package io.github.agentcheck.integration.springai;

import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.model.AgentExecution;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;

import java.util.Objects;
import java.util.function.Function;

public final class SpringAiAgentAdapter implements AgentAdapter {
    private final ChatClient chatClient;
    private final SpringAiResponseMapper responseMapper;

    public SpringAiAgentAdapter(ChatClient chatClient) {
        this(chatClient, Document::getId);
    }

    public SpringAiAgentAdapter(
            ChatClient chatClient,
            Function<Document, String> documentIdExtractor) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        responseMapper = new SpringAiResponseMapper(documentIdExtractor);
    }

    @Override
    public AgentExecution execute(String input) {
        Objects.requireNonNull(input, "input");

        SpringAiToolCallCaptureAdvisor toolCallCapture = new SpringAiToolCallCaptureAdvisor();
        ChatClientResponse response = executePrompt(input, toolCallCapture);
        return responseMapper.map(input, response, toolCallCapture.capturedToolCalls());
    }

    private ChatClientResponse executePrompt(
            String input,
            SpringAiToolCallCaptureAdvisor toolCallCapture) {
        ChatClientResponse response = chatClient.prompt()
                .user(input)
                .advisors(toolCallCapture)
                .call()
                .chatClientResponse();
        if (response == null) {
            throw new IllegalStateException("Spring AI returned no ChatClientResponse");
        }
        return response;
    }
}
