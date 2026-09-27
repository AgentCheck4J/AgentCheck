package io.github.agentcheck.integration.springai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class SpringAiAgentAdapter implements AgentAdapter {
    private static final String RETRIEVED_DOCUMENTS_CONTEXT_KEY = "qa_retrieved_documents";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ChatClient chatClient;
    private final Function<Document, String> documentIdExtractor;

    public SpringAiAgentAdapter(ChatClient chatClient) {
        this(chatClient, Document::getId);
    }

    public SpringAiAgentAdapter(ChatClient chatClient, Function<Document, String> documentIdExtractor) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        this.documentIdExtractor = Objects.requireNonNull(documentIdExtractor, "documentIdExtractor");
    }

    @Override
    public AgentExecution execute(String input) {
        Objects.requireNonNull(input, "input");
        ToolCaptureAdvisor toolCapture = new ToolCaptureAdvisor();
        ChatClientResponse response = chatClient.prompt()
                .user(input)
                .advisors(toolCapture)
                .call()
                .chatClientResponse();
        if (response == null) {
            throw new IllegalStateException("Spring AI returned no ChatClientResponse");
        }
        return new AgentExecution(
                input,
                extractAnswer(response),
                extractDocuments(response),
                toolCapture.toolCalls());
    }

    private String extractAnswer(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        String answer = chatResponse.getResult().getOutput().getText();
        return answer == null ? "" : answer;
    }

    private List<RetrievedDocument> extractDocuments(ChatClientResponse response) {
        Object contextValue = response.context().get(RETRIEVED_DOCUMENTS_CONTEXT_KEY);
        if (contextValue == null) {
            return List.of();
        }
        if (!(contextValue instanceof List<?> values)) {
            throw new IllegalStateException("Spring AI retrieval context must contain a list of documents");
        }
        List<RetrievedDocument> documents = new ArrayList<>();
        int rank = 1;
        for (Object value : values) {
            if (!(value instanceof Document document)) {
                throw new IllegalStateException("Spring AI retrieval context contains a non-document value");
            }
            documents.add(new RetrievedDocument(documentIdExtractor.apply(document), rank, document.getScore()));
            rank++;
        }
        return List.copyOf(documents);
    }

    static final class ToolCaptureAdvisor implements CallAdvisor {
        private final List<ToolCall> toolCalls = new ArrayList<>();

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);
            ChatResponse chatResponse = response.chatResponse();
            if (chatResponse == null || chatResponse.getResult() == null) {
                return response;
            }
            AssistantMessage output = chatResponse.getResult().getOutput();
            if (output == null) {
                return response;
            }
            for (AssistantMessage.ToolCall toolCall : output.getToolCalls()) {
                toolCalls.add(new ToolCall(toolCall.name(), parseArguments(toolCall.arguments())));
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

        List<ToolCall> toolCalls() {
            return List.copyOf(toolCalls);
        }

        private Map<String, Object> parseArguments(String arguments) {
            if (arguments == null || arguments.isBlank()) {
                return Map.of();
            }
            try {
                return JSON.readValue(arguments, new TypeReference<>() { });
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Spring AI tool arguments are not a JSON object", exception);
            }
        }
    }
}
