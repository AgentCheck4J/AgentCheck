package io.github.agentcheck.recording;

import io.github.agentcheck.AgentAdapter;
import io.github.agentcheck.golden.ExpectedBehaviour;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.model.RetrievedDocument;
import io.github.agentcheck.model.ToolCall;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Records observable agent behaviour and proposes disabled golden-test drafts.
 * Recorded behaviour is evidence for human review, not verified ground truth.
 */
public final class GoldenTestRecorder {
    private final RecordingOptions options;

    /**
     * Creates a recorder with privacy-preserving defaults.
     */
    public GoldenTestRecorder() {
        this(RecordingOptions.defaults());
    }

    /**
     * Creates a recorder with explicit output and privacy options.
     *
     * @param options output and privacy options
     */
    public GoldenTestRecorder(RecordingOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Executes every input and immediately converts the recording into a draft.
     *
     * @param agent agent implementation to execute
     * @param suiteName generated suite name
     * @param testInputs inputs to execute in order
     * @return disabled golden-test draft
     */
    public GoldenTestSuiteDraft record(
            AgentAdapter agent,
            String suiteName,
            List<TestInput> testInputs) {
        RecordedTestSuite recording = capture(agent, suiteName, testInputs);
        return fromExecutions(recording);
    }

    /**
     * Executes every input and returns a reusable, serializable recording.
     *
     * @param agent agent implementation to execute
     * @param suiteName recorded suite name
     * @param testInputs inputs to execute in order
     * @return versioned execution recording
     */
    public RecordedTestSuite capture(
            AgentAdapter agent,
            String suiteName,
            List<TestInput> testInputs) {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(testInputs, "testInputs");

        List<RecordedTestCase> recordedCases = testInputs.stream()
                .map(testInput -> execute(agent, testInput))
                .toList();
        return new RecordedTestSuite(suiteName, recordedCases);
    }

    /**
     * Converts a saved or previously captured execution suite into a draft.
     *
     * @param recording execution recording to convert
     * @return disabled golden-test draft
     */
    public GoldenTestSuiteDraft fromExecutions(RecordedTestSuite recording) {
        Objects.requireNonNull(recording, "recording");
        return fromExecutions(recording.suite(), recording.cases());
    }

    /**
     * Converts recorded cases into a draft without executing an agent.
     *
     * @param suiteName generated suite name
     * @param recordedCases executions to convert in order
     * @return disabled golden-test draft
     */
    public GoldenTestSuiteDraft fromExecutions(
            String suiteName,
            List<RecordedTestCase> recordedCases) {
        Objects.requireNonNull(recordedCases, "recordedCases");
        List<GoldenTestCaseDraft> draftCases = recordedCases.stream()
                .map(this::createDraftCase)
                .toList();
        return new GoldenTestSuiteDraft(suiteName, options, draftCases);
    }

    private RecordedTestCase execute(AgentAdapter agent, TestInput testInput) {
        Objects.requireNonNull(testInput, "testInput");
        try {
            AgentExecution execution = agent.execute(testInput.input());
            if (execution == null) {
                throw new IllegalStateException("agent returned no execution");
            }
            return new RecordedTestCase(testInput, execution);
        } catch (RuntimeException exception) {
            throw new GoldenTestRecordingException(testInput.id(), exception);
        }
    }

    private GoldenTestCaseDraft createDraftCase(RecordedTestCase recordedCase) {
        Objects.requireNonNull(recordedCase, "recordedCase");
        TestInput testInput = recordedCase.testInput();
        AgentExecution execution = recordedCase.execution();
        ExpectedBehaviour proposedExpectations = new ExpectedBehaviour(
                extractDocumentIds(execution),
                extractToolNames(execution),
                List.of());
        return new GoldenTestCaseDraft(
                testInput.id(),
                testInput.input(),
                testInput.description(),
                testInput.tags(),
                proposedExpectations,
                execution.answer(),
                execution.toolCalls());
    }

    private List<String> extractDocumentIds(AgentExecution execution) {
        return execution.retrievedDocuments().stream()
                .sorted(Comparator.comparingInt(RetrievedDocument::rank))
                .map(RetrievedDocument::id)
                .distinct()
                .toList();
    }

    private List<String> extractToolNames(AgentExecution execution) {
        Set<String> toolNames = execution.toolCalls().stream()
                .map(ToolCall::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(toolNames);
    }
}
