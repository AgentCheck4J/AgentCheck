package io.github.agentcheck;

import io.github.agentcheck.evaluation.EvaluationStatus;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.model.AgentExecution;
import io.github.agentcheck.report.ConsoleReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldenDatasetV02Test {
    @TempDir Path tempDir;

    @Test
    void parsesDescriptionTagsAndEnabledWhileKeepingOldFieldsOptional() throws Exception {
        GoldenTestSuite suite = GoldenTestSuite.load(write("""
                suite: richer
                cases:
                  - id: smoke-case
                    description: Critical happy path
                    tags: [smoke, support, smoke]
                    enabled: true
                    input: Hello
                  - id: future-case
                    tags: [future]
                    enabled: false
                    input: Later
                  - id: legacy-case
                    input: Legacy
                """));

        assertThat(suite.cases()).hasSize(3);
        assertThat(suite.cases().getFirst().description()).isEqualTo("Critical happy path");
        assertThat(suite.cases().getFirst().tags()).containsExactly("smoke", "support");
        assertThat(suite.cases().get(1).enabled()).isFalse();
        assertThat(suite.cases().get(2).enabled()).isTrue();
        assertThat(suite.cases().get(2).description()).isEmpty();
    }

    @Test
    void disabledCasesAreSkippedAndNeverExecuteTheAdapter() throws Exception {
        GoldenTestSuite suite = GoldenTestSuite.load(write("""
                suite: skip
                cases:
                  - id: active
                    input: Active
                  - id: disabled
                    input: Disabled
                    enabled: false
                """));
        AtomicInteger calls = new AtomicInteger();

        EvaluationSuiteResult result = new AgentCheck().evaluate(input -> {
            calls.incrementAndGet();
            return AgentExecution.of(input, java.util.List.of(), java.util.List.of());
        }, suite);

        assertThat(calls).hasValue(1);
        assertThat(result.cases()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(EvaluationStatus.PASS);
        assertThat(new ConsoleReporter().render(result)).contains("1 skipped");
    }

    @Test
    void selectsCasesMatchingAnyRequestedTag() throws Exception {
        GoldenTestSuite suite = GoldenTestSuite.load(write("""
                suite: tags
                cases:
                  - { id: one, input: One, tags: [smoke] }
                  - { id: two, input: Two, tags: [support] }
                  - { id: three, input: Three, tags: [slow] }
                """));

        assertThat(suite.selectByTags("smoke", "support").cases())
                .extracting(testCase -> testCase.id())
                .containsExactly("one", "two");
        assertThat(suite.selectByTags().cases()).hasSize(3);
    }

    @Test
    void rejectsInvalidMetadataTypes() throws Exception {
        assertThatThrownBy(() -> GoldenTestSuite.load(write("""
                suite: invalid
                cases:
                  - id: broken
                    input: Hello
                    enabled: sometimes
                """))).hasMessageContaining("cases[0].enabled");
    }

    private Path write(String yaml) throws Exception {
        Path path = tempDir.resolve("suite.yaml");
        Files.writeString(path, yaml);
        return path;
    }
}
