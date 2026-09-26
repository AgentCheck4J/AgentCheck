package io.github.agentcheck.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record GoldenTestSuite(String suite, int retrievalK, List<GoldenTestCase> cases, Thresholds thresholds) {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public GoldenTestSuite {
        if (suite == null || suite.isBlank()) throw new IllegalArgumentException("suite name must not be blank");
        if (retrievalK < 1) throw new IllegalArgumentException("retrieval k must be at least 1");
        cases = cases == null ? List.of() : List.copyOf(cases);
        thresholds = thresholds == null ? Thresholds.defaults() : thresholds;
        var ids = new HashSet<String>();
        for (var testCase : cases) {
            if (!ids.add(testCase.id())) throw new IllegalArgumentException("duplicate case id: " + testCase.id());
        }
    }

    public static GoldenTestSuite load(String path) {
        return load(Path.of(path));
    }

    public static GoldenTestSuite load(Path path) {
        try (var input = Files.newInputStream(path)) {
            return parse(YAML.readTree(input));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Could not load golden suite '" + path + "': " + exception.getMessage(), exception);
        }
    }

    static GoldenTestSuite parse(JsonNode root) {
        if (root == null || !root.isObject()) throw new IllegalArgumentException("YAML root must be an object");
        var suite = requiredText(root, "suite", "suite");
        var retrievalK = root.path("retrievalK").asInt(5);
        var caseNodes = root.path("cases");
        if (!caseNodes.isArray()) throw new IllegalArgumentException("'cases' must be a list");
        var cases = new ArrayList<GoldenTestCase>();
        for (int index = 0; index < caseNodes.size(); index++) {
            var node = caseNodes.get(index);
            var location = "cases[" + index + "]";
            var expected = node.path("expected");
            cases.add(new GoldenTestCase(
                    requiredText(node, "id", location + ".id"),
                    requiredText(node, "input", location + ".input"),
                    new ExpectedBehaviour(
                            stringList(expected.path("retrieval").path("relevantDocuments"), location + ".expected.retrieval.relevantDocuments"),
                            stringList(expected.path("tools").path("required"), location + ".expected.tools.required"),
                            stringList(expected.path("tools").path("forbidden"), location + ".expected.tools.forbidden"))));
        }
        return new GoldenTestSuite(suite, retrievalK, cases, parseThresholds(root.path("thresholds"), retrievalK));
    }

    private static Thresholds parseThresholds(JsonNode node, int k) {
        if (node.isMissingNode() || node.isNull()) return Thresholds.defaults();
        var retrieval = node.path("retrieval");
        var recall = nullableDouble(retrieval, "recallAt" + k);
        if (recall == null) recall = nullableDouble(retrieval, "recallAtK");
        return new Thresholds(
                recall,
                nullableDouble(retrieval, "mrr"),
                nullableDouble(node.path("tools"), "accuracy"),
                nullableInt(node.path("policy"), "maxViolations"));
    }

    private static String requiredText(JsonNode node, String field, String location) {
        var value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("'" + location + "' must be a non-blank string");
        }
        return value.asText().strip();
    }

    private static List<String> stringList(JsonNode node, String location) {
        if (node.isMissingNode() || node.isNull()) return List.of();
        if (!node.isArray()) throw new IllegalArgumentException("'" + location + "' must be a list");
        var values = new ArrayList<String>();
        for (var value : node) {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new IllegalArgumentException("'" + location + "' must contain only non-blank strings");
            }
            values.add(value.asText());
        }
        return values;
    }

    private static Double nullableDouble(JsonNode node, String field) {
        var value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        if (!value.isNumber()) throw new IllegalArgumentException("threshold '" + field + "' must be numeric");
        return value.doubleValue();
    }

    private static Integer nullableInt(JsonNode node, String field) {
        var value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        if (!value.canConvertToInt()) throw new IllegalArgumentException("threshold '" + field + "' must be an integer");
        return value.intValue();
    }
}
