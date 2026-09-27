package io.github.agentcheck.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class GoldenTestSuiteParser {
    private static final int DEFAULT_RETRIEVAL_CUTOFF = 5;
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private GoldenTestSuiteParser() {
    }

    static GoldenTestSuite load(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return parse(YAML_MAPPER.readTree(input));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Could not load golden suite '" + path + "': " + exception.getMessage(),
                    exception);
        }
    }

    static GoldenTestSuite parse(JsonNode root) {
        requireObjectRoot(root);

        String suiteName = requiredText(root, "suite", "suite");
        int retrievalCutoff = root.path("retrievalK").asInt(DEFAULT_RETRIEVAL_CUTOFF);
        List<GoldenTestCase> testCases = parseCases(root.path("cases"));
        Thresholds thresholds = parseThresholds(root.path("thresholds"), retrievalCutoff);
        return new GoldenTestSuite(suiteName, retrievalCutoff, testCases, thresholds);
    }

    private static void requireObjectRoot(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("YAML root must be an object");
        }
    }

    private static List<GoldenTestCase> parseCases(JsonNode caseNodes) {
        if (!caseNodes.isArray()) {
            throw new IllegalArgumentException("'cases' must be a list");
        }

        List<GoldenTestCase> testCases = new ArrayList<>();
        for (int index = 0; index < caseNodes.size(); index++) {
            testCases.add(parseCase(caseNodes.get(index), index));
        }
        return List.copyOf(testCases);
    }

    private static GoldenTestCase parseCase(JsonNode caseNode, int index) {
        String location = "cases[" + index + "]";
        JsonNode expectedNode = caseNode.path("expected");
        return new GoldenTestCase(
                requiredText(caseNode, "id", location + ".id"),
                requiredText(caseNode, "input", location + ".input"),
                parseExpectedBehaviour(expectedNode, location),
                optionalText(caseNode, "description", location + ".description"),
                stringList(caseNode.path("tags"), location + ".tags"),
                optionalBoolean(caseNode, "enabled", location + ".enabled", true));
    }

    private static ExpectedBehaviour parseExpectedBehaviour(JsonNode expectedNode, String caseLocation) {
        return new ExpectedBehaviour(
                stringList(
                        expectedNode.path("retrieval").path("relevantDocuments"),
                        caseLocation + ".expected.retrieval.relevantDocuments"),
                stringList(
                        expectedNode.path("tools").path("required"),
                        caseLocation + ".expected.tools.required"),
                stringList(
                        expectedNode.path("tools").path("forbidden"),
                        caseLocation + ".expected.tools.forbidden"));
    }

    private static Thresholds parseThresholds(JsonNode thresholdsNode, int retrievalCutoff) {
        if (isMissing(thresholdsNode)) {
            return Thresholds.defaults();
        }

        JsonNode retrievalThresholds = thresholdsNode.path("retrieval");
        Double recallThreshold = firstDefinedRecallThreshold(retrievalThresholds, retrievalCutoff);
        return new Thresholds(
                recallThreshold,
                nullableDouble(retrievalThresholds, "mrr"),
                nullableDouble(thresholdsNode.path("tools"), "accuracy"),
                nullableInteger(thresholdsNode.path("policy"), "maxViolations"));
    }

    private static Double firstDefinedRecallThreshold(JsonNode retrievalThresholds, int retrievalCutoff) {
        Double cutoffSpecificThreshold = nullableDouble(
                retrievalThresholds,
                "recallAt" + retrievalCutoff);
        if (cutoffSpecificThreshold != null) {
            return cutoffSpecificThreshold;
        }
        return nullableDouble(retrievalThresholds, "recallAtK");
    }

    private static String requiredText(JsonNode node, String fieldName, String location) {
        JsonNode value = node.path(fieldName);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("'" + location + "' must be a non-blank string");
        }
        return value.asText().strip();
    }

    private static String optionalText(JsonNode node, String fieldName, String location) {
        JsonNode value = node.path(fieldName);
        if (isMissing(value)) {
            return "";
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException("'" + location + "' must be a string");
        }
        return value.asText().strip();
    }

    private static boolean optionalBoolean(
            JsonNode node,
            String fieldName,
            String location,
            boolean defaultValue) {
        JsonNode value = node.path(fieldName);
        if (isMissing(value)) {
            return defaultValue;
        }
        if (!value.isBoolean()) {
            throw new IllegalArgumentException("'" + location + "' must be true or false");
        }
        return value.booleanValue();
    }

    private static List<String> stringList(JsonNode node, String location) {
        if (isMissing(node)) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("'" + location + "' must be a list");
        }

        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            values.add(nonBlankText(value, location));
        }
        return List.copyOf(values);
    }

    private static String nonBlankText(JsonNode value, String location) {
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "'" + location + "' must contain only non-blank strings");
        }
        return value.asText();
    }

    private static Double nullableDouble(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (isMissing(value)) {
            return null;
        }
        if (!value.isNumber()) {
            throw new IllegalArgumentException("threshold '" + fieldName + "' must be numeric");
        }
        return value.doubleValue();
    }

    private static Integer nullableInteger(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (isMissing(value)) {
            return null;
        }
        if (!value.canConvertToInt()) {
            throw new IllegalArgumentException("threshold '" + fieldName + "' must be an integer");
        }
        return value.intValue();
    }

    private static boolean isMissing(JsonNode node) {
        return node.isMissingNode() || node.isNull();
    }
}
