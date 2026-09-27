# Changelog

All notable changes will be documented here. This project follows semantic
versioning once releases begin.

## [Unreleased]

### Added

- Optional Spring AI 2.0.1 adapter for blocking `ChatClient` executions
- Mapping of final answers, QuestionAnswerAdvisor documents, similarity scores,
  and tool calls into AgentCheck execution traces
- Custom Spring AI document ID extraction for stable golden-test identifiers

## [0.2.0] - 2026-09-26

### Added

- Baseline/current regression comparison with explicit metric and count deltas
- JSON loading for saved evaluation baselines
- Optional golden-case descriptions, tags, disabling, and tag selection
- Skipped-case reporting without invoking the agent adapter

## [0.1.0]

### Added

- Golden YAML suites and immutable execution models
- Recall@k, reciprocal rank, and MRR
- Required, forbidden, and unexpected tool evaluation
- Explicit forbidden-tool policy violations
- Threshold-based suite status, JSON output, and console reporting
- Deterministic customer-support example
