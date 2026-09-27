# Changelog

All notable changes will be documented here. This project follows semantic
versioning once releases begin.

## [Unreleased]

### Added

- Optional `agentcheck-mcp` module based on the official MCP Java SDK 2.0.1
- Deterministic mapping of ordered MCP tool requests and JSON arguments into
  AgentCheck execution traces
- Non-published examples module with a runnable MCP mapping example

### Changed

- Removed customer-support demo classes and application concerns from
  `agentcheck-core`

### Fixed

- Preserve valid JSON `null` values in captured tool arguments

## [0.4.0] - 2026-09-27

### Added

- Safe golden-test recording from live agents or existing execution traces
- Disabled YAML drafts with proposed document and tool expectations for manual
  review and approval
- Opt-in recording of observed answers and tool arguments
- Non-overwriting draft file output
- Versioned JSON persistence for reusable execution recordings
- Runnable `recordExample` task and versioned deterministic YAML output
- Case-specific recording failures without partial draft output
- Optional Spring AI 2.0.1 adapter for blocking `ChatClient` executions
- Mapping of final answers, QuestionAnswerAdvisor documents, similarity scores,
  and tool calls into AgentCheck execution traces
- Custom Spring AI document ID extraction for stable golden-test identifiers

### Changed

- Split the framework-free core and optional Spring AI integration into the
  `agentcheck-core` and `agentcheck-spring-ai` modules
- Replaced the Spring AI `compileOnly` dependency with an explicit API
  dependency in the integration module
- Split evaluation, suite aggregation, regression comparison, golden-suite
  parsing, Spring AI mapping, and console rendering into focused components
- Standardized explicit types, intention-revealing names, validation methods,
  and immutable intermediate results throughout the codebase

### Fixed

- Run application and recording example tasks with the configured JDK 21
  toolchain even when Gradle itself was started with an older Java runtime

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
