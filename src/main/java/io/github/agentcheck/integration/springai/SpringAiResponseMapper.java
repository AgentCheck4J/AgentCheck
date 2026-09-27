package io.github.agentcheck.integration.springai;

import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class SpringAiResponseMapper {
    private static final String RETRIEVED_DOCUMENTS_CONTEXT_KEY = "qa_retrieved_documents";

    private final Function<Document, String> documentIdExtractor;

    SpringAiResponseMapper(Function<Document, String> documentIdExtractor) {
        this.documentIdExtractor = Objects.requireNonNull(documentIdExtractor, "documentIdExtractor");
    }

    AgentExecution map(String input, ChatClientResponse response, List<ToolCall> toolCalls) {
        return new AgentExecution(input, extractAnswer(response), extractDocuments(response), toolCalls);
    }

    private String extractAnswer(ChatClientResponse response) {
        AssistantMessage assistantMessage = extractAssistantMessage(response.chatResponse());
        if (assistantMessage == null || assistantMessage.getText() == null) {
            return "";
        }
        return assistantMessage.getText();
    }

    private AssistantMessage extractAssistantMessage(ChatResponse response) {
        if (response == null) {
            return null;
        }
        Generation generation = response.getResult();
        if (generation == null) {
            return null;
        }
        return generation.getOutput();
    }

    private List<RetrievedDocument> extractDocuments(ChatClientResponse response) {
        Object contextValue = response.context().get(RETRIEVED_DOCUMENTS_CONTEXT_KEY);
        if (contextValue == null) {
            return List.of();
        }
        if (!(contextValue instanceof List<?> contextItems)) {
            throw new IllegalStateException("Spring AI retrieval context must contain a list of documents");
        }
        return mapDocuments(contextItems);
    }

    private List<RetrievedDocument> mapDocuments(List<?> contextItems) {
        List<RetrievedDocument> retrievedDocuments = new ArrayList<>();
        for (int index = 0; index < contextItems.size(); index++) {
            Document document = requireDocument(contextItems.get(index));
            retrievedDocuments.add(mapDocument(document, index + 1));
        }
        return List.copyOf(retrievedDocuments);
    }

    private Document requireDocument(Object contextItem) {
        if (!(contextItem instanceof Document document)) {
            throw new IllegalStateException("Spring AI retrieval context contains a non-document value");
        }
        return document;
    }

    private RetrievedDocument mapDocument(Document document, int rank) {
        String documentId = documentIdExtractor.apply(document);
        return new RetrievedDocument(documentId, rank, document.getScore());
    }
}
