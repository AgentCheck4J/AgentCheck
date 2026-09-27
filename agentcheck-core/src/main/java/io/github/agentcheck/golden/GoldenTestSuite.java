package io.github.agentcheck.golden;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable golden test suite and its aggregate thresholds.
 *
 * @param suite suite name
 * @param retrievalK retrieval cutoff used for every case
 * @param cases ordered golden test cases
 * @param thresholds aggregate pass/fail thresholds
 */
public record GoldenTestSuite(
        String suite,
        int retrievalK,
        List<GoldenTestCase> cases,
        Thresholds thresholds) {

    /** Validates and defensively copies the suite definition. */
    public GoldenTestSuite {
        requireSuiteName(suite);
        requirePositiveRetrievalCutoff(retrievalK);
        cases = cases == null ? List.of() : List.copyOf(cases);
        requireUniqueCaseIds(cases);
        thresholds = thresholds == null ? Thresholds.defaults() : thresholds;
    }

    /**
     * Loads a YAML suite from a path string.
     *
     * @param path YAML file path
     * @return parsed golden test suite
     */
    public static GoldenTestSuite load(String path) {
        return load(Path.of(path));
    }

    /**
     * Loads a YAML suite from a path.
     *
     * @param path YAML file
     * @return parsed golden test suite
     */
    public static GoldenTestSuite load(Path path) {
        return GoldenTestSuiteParser.load(path);
    }

    /**
     * Selects cases matching at least one requested tag.
     *
     * @param selectedTags tags to match; no tags returns this suite unchanged
     * @return a suite containing only matching cases
     */
    public GoldenTestSuite selectByTags(String... selectedTags) {
        if (selectedTags == null || selectedTags.length == 0) {
            return this;
        }
        requireValidTags(selectedTags);

        Set<String> requestedTags = new HashSet<>(List.of(selectedTags));
        List<GoldenTestCase> selectedCases = cases.stream()
                .filter(testCase -> hasAnyTag(testCase, requestedTags))
                .toList();
        return new GoldenTestSuite(suite, retrievalK, selectedCases, thresholds);
    }

    static GoldenTestSuite parse(JsonNode root) {
        return GoldenTestSuiteParser.parse(root);
    }

    private static void requireSuiteName(String suiteName) {
        if (suiteName == null || suiteName.isBlank()) {
            throw new IllegalArgumentException("suite name must not be blank");
        }
    }

    private static void requirePositiveRetrievalCutoff(int retrievalCutoff) {
        if (retrievalCutoff < 1) {
            throw new IllegalArgumentException("retrieval k must be at least 1");
        }
    }

    private static void requireUniqueCaseIds(List<GoldenTestCase> testCases) {
        Set<String> uniqueCaseIds = new HashSet<>();
        for (GoldenTestCase testCase : testCases) {
            if (!uniqueCaseIds.add(testCase.id())) {
                throw new IllegalArgumentException("duplicate case id: " + testCase.id());
            }
        }
    }

    private static void requireValidTags(String[] selectedTags) {
        boolean containsBlankTag = Arrays.stream(selectedTags)
                .anyMatch(tag -> tag == null || tag.isBlank());
        if (containsBlankTag) {
            throw new IllegalArgumentException("selected tag must not be blank");
        }
    }

    private boolean hasAnyTag(GoldenTestCase testCase, Set<String> requestedTags) {
        return testCase.tags().stream().anyMatch(requestedTags::contains);
    }
}
