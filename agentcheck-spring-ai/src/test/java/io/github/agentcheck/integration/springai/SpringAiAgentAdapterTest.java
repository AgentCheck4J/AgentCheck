package io.github.agentcheck.integration.springai;

import io.github.agentcheck.model.AgentExecution;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiAgentAdapterTest {
    @Test
    void mapsTheInputAndAnswerFromAChatClient() {
        ChatModel chatModel = prompt -> response("Your order is in transit.");
        SpringAiAgentAdapter adapter = new SpringAiAgentAdapter(ChatClient.create(chatModel));

        AgentExecution execution = adapter.execute("Where is my order?");

        assertThat(execution.input()).isEqualTo("Where is my order?");
        assertThat(execution.answer()).isEqualTo("Your order is in transit.");
        assertThat(execution.retrievedDocuments()).isEmpty();
        assertThat(execution.toolCalls()).isEmpty();
    }

    @Test
    void mapsQuestionAnswerAdvisorDocumentsInTheirReturnedOrder() {
        Document first = Document.builder()
                .id("generated-1")
                .text("Shipping policy")
                .metadata("source", "shipping-policy.md")
                .score(0.91)
                .build();
        Document second = Document.builder()
                .id("generated-2")
                .text("Returns policy")
                .metadata("source", "returns-policy.md")
                .score(0.73)
                .build();
        CallAdvisor retrievalContext = new RetrievalContextAdvisor(List.of(first, second));
        ChatModel chatModel = prompt -> response("Answer based on retrieved documents.");
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(retrievalContext).build();
        SpringAiAgentAdapter adapter = new SpringAiAgentAdapter(
                chatClient,
                document -> (String) document.getMetadata().get("source"));
        AgentExecution execution = adapter.execute("What is the policy?");

        assertThat(execution.retrievedDocuments())
                .extracting(document -> document.id(), document -> document.rank(), document -> document.score())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("shipping-policy.md", 1, 0.91),
                        org.assertj.core.groups.Tuple.tuple("returns-policy.md", 2, 0.73));
    }

    @Test
    void capturesToolNamesAndJsonArguments() {
        AssistantMessage.ToolCall springToolCall = new AssistantMessage.ToolCall(
                "call-1", "function", "get_order", "{\"orderId\":42,\"includeHistory\":true}");
        ChatClientResponse response = responseWithToolCall(springToolCall);
        SpringAiToolCallCaptureAdvisor advisor = new SpringAiToolCallCaptureAdvisor();
        CallAdvisorChain chain = new FixedResponseChain(response);

        advisor.adviseCall(new ChatClientRequest(new Prompt("question"), Map.of()), chain);

        assertThat(advisor.capturedToolCalls()).hasSize(1);
        assertThat(advisor.capturedToolCalls().getFirst().name()).isEqualTo("get_order");
        assertThat(advisor.capturedToolCalls().getFirst().arguments())
                .containsEntry("orderId", 42)
                .containsEntry("includeHistory", true);
    }

    @Test
    void rejectsToolArgumentsThatAreNotAJsonObject() {
        AssistantMessage.ToolCall springToolCall = new AssistantMessage.ToolCall(
                "call-1", "function", "get_order", "[42]");
        SpringAiToolCallCaptureAdvisor advisor = new SpringAiToolCallCaptureAdvisor();
        CallAdvisorChain chain = new FixedResponseChain(responseWithToolCall(springToolCall));

        assertThatThrownBy(() -> advisor.adviseCall(
                new ChatClientRequest(new Prompt("question"), Map.of()), chain))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Spring AI tool arguments are not a JSON object");
    }

    private static ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    private static ChatClientResponse responseWithToolCall(AssistantMessage.ToolCall toolCall) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(toolCall))
                .build();
        return new ChatClientResponse(new ChatResponse(List.of(new Generation(message))), Map.of());
    }

    private static final class RetrievalContextAdvisor implements CallAdvisor {
        private final List<Document> documents;

        private RetrievalContextAdvisor(List<Document> documents) {
            this.documents = List.copyOf(documents);
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            ChatClientResponse response = chain.nextCall(request);
            return response.mutate().context("qa_retrieved_documents", documents).build();
        }

        @Override
        public String getName() {
            return "Test retrieval context";
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    private static final class FixedResponseChain implements CallAdvisorChain {
        private final ChatClientResponse response;

        private FixedResponseChain(ChatClientResponse response) {
            this.response = response;
        }

        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            return response;
        }

        @Override
        public List<CallAdvisor> getCallAdvisors() {
            return List.of();
        }

        @Override
        public CallAdvisorChain copy(CallAdvisor advisor) {
            return this;
        }
    }
}
