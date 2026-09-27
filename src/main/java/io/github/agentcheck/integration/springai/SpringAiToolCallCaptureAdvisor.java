package io.github.agentcheck.integration.springai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.agentcheck.model.ToolCall;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SpringAiToolCallCaptureAdvisor implements CallAdvisor {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final List<ToolCall> capturedToolCalls = new ArrayList<>();

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        AssistantMessage assistantMessage = extractAssistantMessage(response);
        if (assistantMessage != null) {
            captureToolCalls(assistantMessage.getToolCalls());
        }
        return response;
    }

    @Override
    public String getName() {
        return SpringAiAgentAdapter.class.getSimpleName() + "ToolCapture";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }

    List<ToolCall> capturedToolCalls() {
        return List.copyOf(capturedToolCalls);
    }

    private AssistantMessage extractAssistantMessage(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return null;
        }
        Generation generation = chatResponse.getResult();
        if (generation == null) {
            return null;
        }
        return generation.getOutput();
    }

    private void captureToolCalls(List<AssistantMessage.ToolCall> springToolCalls) {
        springToolCalls.stream()
                .map(this::mapToolCall)
                .forEach(capturedToolCalls::add);
    }

    private ToolCall mapToolCall(AssistantMessage.ToolCall springToolCall) {
        return new ToolCall(
                springToolCall.name(),
                parseArguments(springToolCall.arguments()));
    }

    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return JSON_MAPPER.readValue(arguments, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Spring AI tool arguments are not a JSON object", exception);
        }
    }
}
