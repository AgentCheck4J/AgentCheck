package io.github.agentcheck.recording;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.model.ToolCall;

import java.util.List;
import java.util.Map;

final class GoldenTestSuiteDraftWriter {
    private static final YAMLFactory YAML_FACTORY = YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);

    private final RecordingOptions options;

    GoldenTestSuiteDraftWriter(RecordingOptions options) {
        this.options = options;
    }

    String write(GoldenTestSuiteDraft draft) {
        try {
            return YAML_MAPPER.writeValueAsString(createRoot(draft));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize golden suite draft", exception);
        }
    }

    private ObjectNode createRoot(GoldenTestSuiteDraft draft) {
        ObjectNode root = YAML_MAPPER.createObjectNode();
        root.put("schemaVersion", GoldenTestSuiteDraft.CURRENT_SCHEMA_VERSION);
        root.put("suite", draft.suite());
        root.put("retrievalK", draft.retrievalK());
        root.put("draft", true);
        ArrayNode cases = root.putArray("cases");
        draft.cases().forEach(testCase -> cases.add(createCase(testCase)));
        return root;
    }

    private ObjectNode createCase(GoldenTestCaseDraft testCase) {
        ObjectNode caseNode = YAML_MAPPER.createObjectNode();
        caseNode.put("id", testCase.id());
        addDescription(caseNode, testCase.description());
        addStringList(caseNode, "tags", testCase.tags());
        caseNode.put("enabled", false);
        caseNode.put("input", testCase.input());
        caseNode.set("expected", createExpectations(testCase.proposedExpectations()));
        addObservations(caseNode, testCase);
        return caseNode;
    }

    private void addDescription(ObjectNode caseNode, String description) {
        if (!description.isBlank()) {
            caseNode.put("description", description);
        }
    }

    private ObjectNode createExpectations(ExpectedBehaviour expectations) {
        ObjectNode expected = YAML_MAPPER.createObjectNode();
        ObjectNode retrieval = expected.putObject("retrieval");
        addStringList(retrieval, "relevantDocuments", expectations.relevantDocuments());
        ObjectNode tools = expected.putObject("tools");
        addStringList(tools, "required", expectations.requiredTools());
        addStringList(tools, "forbidden", expectations.forbiddenTools());
        return expected;
    }

    private void addObservations(ObjectNode caseNode, GoldenTestCaseDraft testCase) {
        boolean includeAnswer = options.includeObservedAnswers();
        boolean hasToolCalls = !testCase.observedToolCalls().isEmpty();
        if (!includeAnswer && !hasToolCalls) {
            return;
        }

        ObjectNode observed = caseNode.putObject("observed");
        if (includeAnswer) {
            observed.put("answer", testCase.observedAnswer());
        }
        if (hasToolCalls) {
            addObservedToolCalls(observed, testCase.observedToolCalls());
        }
    }

    private void addObservedToolCalls(ObjectNode observed, List<ToolCall> toolCalls) {
        ArrayNode tools = observed.putArray("tools");
        for (ToolCall toolCall : toolCalls) {
            ObjectNode tool = tools.addObject();
            tool.put("name", toolCall.name());
            addToolArguments(tool, toolCall.arguments());
        }
    }

    private void addToolArguments(ObjectNode tool, Map<String, Object> arguments) {
        if (options.includeObservedToolArguments() && !arguments.isEmpty()) {
            tool.set("arguments", YAML_MAPPER.valueToTree(arguments));
        }
    }

    private void addStringList(ObjectNode parent, String fieldName, List<String> values) {
        ArrayNode array = parent.putArray(fieldName);
        values.forEach(array::add);
    }
}
