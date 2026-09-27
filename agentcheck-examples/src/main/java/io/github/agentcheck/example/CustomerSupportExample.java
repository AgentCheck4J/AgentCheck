package io.github.agentcheck.example;

import io.github.agentcheck.AgentCheck;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.evaluation.EvaluationSuiteResult;
import io.github.agentcheck.report.ConsoleReporter;

import java.nio.file.Path;

/** Runs the deterministic customer-support evaluation example. */
public final class CustomerSupportExample {
    private CustomerSupportExample() { }

    /**
     * Runs the example.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        Path suitePath = arguments.length == 0
                ? Path.of("examples/customer-support/golden.yaml")
                : Path.of(arguments[0]);
        GoldenTestSuite suite = GoldenTestSuite.load(suitePath);
        EvaluationSuiteResult result = new AgentCheck().evaluate(new FakeSupportAgent(), suite);
        new ConsoleReporter().print(result, System.out);
    }
}
