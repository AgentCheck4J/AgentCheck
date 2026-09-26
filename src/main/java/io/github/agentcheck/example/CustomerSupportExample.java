package io.github.agentcheck.example;

import io.github.agentcheck.AgentCheck;
import io.github.agentcheck.golden.GoldenTestSuite;
import io.github.agentcheck.report.ConsoleReporter;

import java.nio.file.Path;

public final class CustomerSupportExample {
    private CustomerSupportExample() { }

    public static void main(String[] args) {
        var suitePath = args.length == 0
                ? Path.of("examples/customer-support/golden.yaml")
                : Path.of(args[0]);
        var result = new AgentCheck().evaluate(new FakeSupportAgent(), GoldenTestSuite.load(suitePath));
        new ConsoleReporter().print(result, System.out);
    }
}
