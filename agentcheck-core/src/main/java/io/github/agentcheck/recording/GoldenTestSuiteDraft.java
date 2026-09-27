package io.github.agentcheck.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A reviewable golden-suite proposal. Every serialized case is disabled until
 * a person explicitly approves its expectations.
 */
public final class GoldenTestSuiteDraft {
    /** Current version of the generated YAML draft format. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final String suite;
    private final RecordingOptions options;
    private final List<GoldenTestCaseDraft> cases;

    GoldenTestSuiteDraft(
            String suite,
            RecordingOptions options,
            List<GoldenTestCaseDraft> cases) {
        requireNonBlank(suite, "draft suite name");
        this.suite = suite;
        this.options = Objects.requireNonNull(options, "options");
        this.cases = cases == null ? List.of() : List.copyOf(cases);
        requireUniqueCaseIds(this.cases);
    }

    /**
     * Returns the proposed suite name.
     *
     * @return proposed suite name
     */
    public String suite() {
        return suite;
    }

    /**
     * Returns the retrieval cutoff written to the generated suite.
     *
     * @return retrieval cutoff
     */
    public int retrievalK() {
        return options.retrievalK();
    }

    /**
     * Returns the immutable cases awaiting review.
     *
     * @return immutable draft cases
     */
    public List<GoldenTestCaseDraft> cases() {
        return cases;
    }

    /**
     * Serializes this draft as deterministic YAML.
     *
     * @return versioned YAML draft
     */
    public String toYaml() {
        return new GoldenTestSuiteDraftWriter(options).write(this);
    }

    /**
     * Writes this draft to a new file and refuses to overwrite an existing one.
     *
     * @param path destination that must not exist
     * @throws IOException if the draft cannot be written
     */
    public void writeNew(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Files.writeString(path, toYaml(), StandardOpenOption.CREATE_NEW);
    }

    private static void requireUniqueCaseIds(List<GoldenTestCaseDraft> testCases) {
        Set<String> uniqueCaseIds = new HashSet<>();
        for (GoldenTestCaseDraft testCase : testCases) {
            if (!uniqueCaseIds.add(testCase.id())) {
                throw new IllegalArgumentException("duplicate draft case id: " + testCase.id());
            }
        }
    }

    private static void requireNonBlank(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
    }
}
