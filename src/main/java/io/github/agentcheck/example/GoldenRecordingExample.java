package io.github.agentcheck.example;

import io.github.agentcheck.recording.GoldenTestRecorder;
import io.github.agentcheck.recording.GoldenTestSuiteDraft;
import io.github.agentcheck.recording.TestInput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Demonstrates safe golden-test draft generation with the deterministic fake agent. */
public final class GoldenRecordingExample {
    private static final Path DEFAULT_OUTPUT = Path.of(
            "build",
            "agentcheck",
            "customer-support-draft.yaml");

    private GoldenRecordingExample() {
    }

    /**
     * Records the example suite to the default path or the first argument.
     *
     * @param arguments optional output path as the first argument
     * @throws IOException if the draft cannot be written
     */
    public static void main(String[] arguments) throws IOException {
        Path outputPath = arguments.length == 0 ? DEFAULT_OUTPUT : Path.of(arguments[0]);
        List<TestInput> testInputs = List.of(
                new TestInput("shipping-status", "Where is my order?"),
                new TestInput("refund", "Refund me"));

        GoldenTestSuiteDraft draft = new GoldenTestRecorder().record(
                new FakeSupportAgent(),
                "customer-support-recorded",
                testInputs);

        Files.createDirectories(outputPath.toAbsolutePath().getParent());
        draft.writeNew(outputPath);
        System.out.println("Golden-test draft written to " + outputPath.toAbsolutePath());
    }
}
