package io.github.agentcheck.recording;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldenTestRecorderTest {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @TempDir
    Path tempDirectory;

    @Test
    void recordsLiveExecutionsInInputOrder() throws Exception {
        List<String> receivedInputs = new ArrayList<>();
        AgentAdapter agent = input -> {
            receivedInputs.add(input);
            return execution(input);
        };
        List<TestInput> testInputs = List.of(
                new TestInput("shipping", "Where is my order?"),
                new TestInput("refund", "Refund my order"));

        GoldenTestSuiteDraft draft = new GoldenTestRecorder().record(agent, "support", testInputs);

        assertThat(receivedInputs).containsExactly("Where is my order?", "Refund my order");
        assertThat(draft.cases()).extracting(GoldenTestCaseDraft::id)
                .containsExactly("shipping", "refund");
    }

    @Test
    void proposesRankedUniqueDocumentsAndDistinctToolNames() {
        TestInput testInput = new TestInput("shipping", "Where is my order?");
        AgentExecution execution = new AgentExecution(
                testInput.input(),
                "Your order is in transit.",
                List.of(
                        new RetrievedDocument("refund.md", 2),
                        new RetrievedDocument("shipping.md", 1),
                        new RetrievedDocument("shipping.md", 3)),
                List.of(
                        new ToolCall("get_order"),
                        new ToolCall("get_status"),
                        new ToolCall("get_order")));

        GoldenTestSuiteDraft draft = new GoldenTestRecorder().fromExecutions(
                "support",
                List.of(new RecordedTestCase(testInput, execution)));

        assertThat(draft.cases().getFirst().proposedExpectations().relevantDocuments())
                .containsExactly("shipping.md", "refund.md");
        assertThat(draft.cases().getFirst().proposedExpectations().requiredTools())
                .containsExactly("get_order", "get_status");
        assertThat(draft.cases().getFirst().proposedExpectations().forbiddenTools()).isEmpty();
    }

    @Test
    void producesDisabledYamlThatCanBeLoadedAsAGoldenSuite() throws Exception {
        TestInput testInput = new TestInput(
                "shipping",
                "Where is my order?",
                "Record the current shipping behaviour",
                List.of("support", "draft"));
        GoldenTestSuiteDraft draft = new GoldenTestRecorder().fromExecutions(
                "support",
                List.of(new RecordedTestCase(testInput, execution(testInput.input()))));
        Path draftPath = tempDirectory.resolve("golden-draft.yaml");

        draft.writeNew(draftPath);
        GoldenTestSuite loadedSuite = GoldenTestSuite.load(draftPath);
        JsonNode yaml = YAML_MAPPER.readTree(Files.readString(draftPath));

        assertThat(yaml.path("draft").booleanValue()).isTrue();
        assertThat(loadedSuite.cases()).singleElement().satisfies(testCase -> {
            assertThat(testCase.enabled()).isFalse();
            assertThat(testCase.description()).isEqualTo("Record the current shipping behaviour");
            assertThat(testCase.tags()).containsExactly("support", "draft");
        });
    }

    @Test
    void safeDefaultsExcludeAnswerAndToolArguments() throws Exception {
        GoldenTestSuiteDraft draft = draftWithSensitiveObservations(RecordingOptions.defaults());

        JsonNode yaml = YAML_MAPPER.readTree(draft.toYaml());
        JsonNode observed = yaml.path("cases").get(0).path("observed");

        assertThat(observed.has("answer")).isFalse();
        assertThat(observed.path("tools").get(0).path("name").asText()).isEqualTo("get_order");
        assertThat(observed.path("tools").get(0).has("arguments")).isFalse();
    }

    @Test
    void generatedYamlMatchesTheVersionedDraftFormat() {
        GoldenTestSuiteDraft draft = draftWithSensitiveObservations(RecordingOptions.defaults());

        assertThat(draft.toYaml()).isEqualTo("""
                schemaVersion: 1
                suite: "support"
                retrievalK: 5
                draft: true
                cases:
                - id: "shipping"
                  tags: []
                  enabled: false
                  input: "Where is my order?"
                  expected:
                    retrieval:
                      relevantDocuments:
                      - "shipping.md"
                    tools:
                      required:
                      - "get_order"
                      forbidden: []
                  observed:
                    tools:
                    - name: "get_order"
                """);
    }

    @Test
    void observationsCanBeIncludedExplicitly() throws Exception {
        RecordingOptions options = new RecordingOptions(3, true, true);
        GoldenTestSuiteDraft draft = draftWithSensitiveObservations(options);

        JsonNode yaml = YAML_MAPPER.readTree(draft.toYaml());
        JsonNode testCase = yaml.path("cases").get(0);

        assertThat(yaml.path("retrievalK").asInt()).isEqualTo(3);
        assertThat(testCase.path("observed").path("answer").asText())
                .isEqualTo("Order 42 belongs to Valerie.");
        assertThat(testCase.path("observed").path("tools").get(0).path("arguments").path("orderId").asInt())
                .isEqualTo(42);
    }

    @Test
    void writeNewNeverOverwritesAnExistingFile() throws Exception {
        Path existingPath = tempDirectory.resolve("golden.yaml");
        Files.writeString(existingPath, "existing content");
        GoldenTestSuiteDraft draft = draftWithSensitiveObservations(RecordingOptions.defaults());

        assertThatThrownBy(() -> draft.writeNew(existingPath))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readString(existingPath)).isEqualTo("existing content");
    }

    @Test
    void rejectsRecordedExecutionsWithDifferentInputs() {
        TestInput testInput = new TestInput("shipping", "Where is my order?");
        AgentExecution execution = execution("Different input");

        assertThatThrownBy(() -> new RecordedTestCase(testInput, execution))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match test input 'shipping'");
    }

    @Test
    void rejectsDuplicateDraftCaseIds() {
        RecordedTestCase first = new RecordedTestCase(
                new TestInput("duplicate", "First input"),
                execution("First input"));
        RecordedTestCase second = new RecordedTestCase(
                new TestInput("duplicate", "Second input"),
                execution("Second input"));

        assertThatThrownBy(() -> new GoldenTestRecorder().fromExecutions("suite", List.of(first, second)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate draft case id: duplicate");
    }

    @Test
    void reportsTheFailingInputWhenAgentExecutionThrows() {
        AgentAdapter failingAgent = input -> {
            throw new IllegalStateException("model request timed out");
        };
        TestInput testInput = new TestInput("shipping", "Where is my order?");

        assertThatThrownBy(() -> new GoldenTestRecorder().record(
                failingAgent,
                "support",
                List.of(testInput)))
                .isInstanceOf(GoldenTestRecordingException.class)
                .hasMessage("Could not record test input 'shipping': model request timed out")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordedExecutionsCanBeSavedLoadedAndConvertedLater() throws Exception {
        TestInput testInput = new TestInput("shipping", "Where is my order?");
        GoldenTestRecorder recorder = new GoldenTestRecorder();
        RecordedTestSuite recording = recorder.capture(
                input -> execution(input),
                "support",
                List.of(testInput));
        Path recordingPath = tempDirectory.resolve("recording.json");

        recording.writeNew(recordingPath);
        RecordedTestSuite loadedRecording = RecordedTestSuite.loadJson(recordingPath);
        GoldenTestSuiteDraft draft = recorder.fromExecutions(loadedRecording);

        assertThat(loadedRecording).isEqualTo(recording);
        assertThat(draft.suite()).isEqualTo("support");
        assertThat(draft.cases()).extracting(GoldenTestCaseDraft::id).containsExactly("shipping");
    }

    @Test
    void rejectsUnsupportedRecordingSchemaVersions() {
        assertThatThrownBy(() -> new RecordedTestSuite(99, "support", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported recorded test suite schema version: 99");
    }

    private GoldenTestSuiteDraft draftWithSensitiveObservations(RecordingOptions options) {
        TestInput testInput = new TestInput("shipping", "Where is my order?");
        AgentExecution execution = new AgentExecution(
                testInput.input(),
                "Order 42 belongs to Valerie.",
                List.of(new RetrievedDocument("shipping.md", 1)),
                List.of(new ToolCall("get_order", Map.of("orderId", 42))));
        return new GoldenTestRecorder(options).fromExecutions(
                "support",
                List.of(new RecordedTestCase(testInput, execution)));
    }

    private AgentExecution execution(String input) {
        return new AgentExecution(
                input,
                "Your order is in transit.",
                List.of(new RetrievedDocument("shipping.md", 1)),
                List.of(new ToolCall("get_order")));
    }
}
