# Contributing to AgentCheck

Thank you for helping keep deterministic agent testing small and useful.

1. Open an issue for significant API changes.
2. Use JDK 21 and run `./gradlew test`.
3. Add focused tests for behavioural changes and edge cases.
4. Keep expected behaviour separate from actual execution data.
5. Avoid provider-specific dependencies in the core.
6. Use explicit Java types; do not use `var` or wildcard imports.
7. Prefer small methods, intention-revealing names, and classes with one clear
   responsibility.

By contributing, you agree that your contribution is licensed under the MIT License.
