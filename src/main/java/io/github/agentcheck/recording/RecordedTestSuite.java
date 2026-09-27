package io.github.agentcheck.recording;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A versioned, serializable set of agent executions captured before golden
 * expectations are reviewed.
 *
 * @param schemaVersion recording file format version
 * @param suite suite name carried into the generated draft
 * @param cases immutable recorded cases
 */
public record RecordedTestSuite(
        int schemaVersion,
        String suite,
        List<RecordedTestCase> cases) {

    /** Current version of the recording JSON format. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** Validates schema compatibility and unique recorded case identifiers. */
    public RecordedTestSuite {
        requireSupportedSchemaVersion(schemaVersion);
        requireNonBlank(suite, "recorded suite name");
        cases = cases == null ? List.of() : List.copyOf(cases);
        requireUniqueCaseIds(cases);
    }

    /**
     * Creates a recording using the current schema version.
     *
     * @param suite suite name carried into the generated draft
     * @param cases recorded cases
     */
    public RecordedTestSuite(String suite, List<RecordedTestCase> cases) {
        this(CURRENT_SCHEMA_VERSION, suite, cases);
    }

    /**
     * Serializes this recording as stable, readable JSON.
     *
     * @return formatted JSON recording
     */
    public String toJson() {
        try {
            return JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize recorded test suite", exception);
        }
    }

    /**
     * Writes a new recording and refuses to overwrite an existing file.
     *
     * @param path destination that must not exist
     * @throws IOException if the recording cannot be written
     */
    public void writeNew(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Files.writeString(path, toJson(), StandardOpenOption.CREATE_NEW);
    }

    /**
     * Loads a previously saved recording.
     *
     * @param path recording JSON path
     * @return validated recorded test suite
     */
    public static RecordedTestSuite loadJson(Path path) {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            return JSON_MAPPER.readValue(input, RecordedTestSuite.class);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Could not load recorded test suite '" + path + "': " + exception.getMessage(),
                    exception);
        }
    }

    private static void requireSupportedSchemaVersion(int schemaVersion) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "unsupported recorded test suite schema version: " + schemaVersion);
        }
    }

    private static void requireUniqueCaseIds(List<RecordedTestCase> recordedCases) {
        Set<String> uniqueCaseIds = new HashSet<>();
        for (RecordedTestCase recordedCase : recordedCases) {
            if (!uniqueCaseIds.add(recordedCase.testInput().id())) {
                throw new IllegalArgumentException(
                        "duplicate recorded test case id: " + recordedCase.testInput().id());
            }
        }
    }

    private static void requireNonBlank(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
    }
}
