# AgentCheck development guidelines

Apply these rules to every future code change in this repository:

- Follow Clean Code principles and preserve existing behaviour unless a change
  is explicitly requested.
- Use explicit Java types. Do not use `var` or wildcard imports.
- Prefer small methods with one clear responsibility.
- Use intention-revealing names for classes, methods, variables, and tests.
- Keep public facades small and move distinct responsibilities into focused,
  package-private components where appropriate.
- Keep framework-specific integrations outside the deterministic core.
- Prefer immutable values and defensive copies at API boundaries.
- Validate inputs early and provide precise error messages.
- Avoid deeply nested control flow, compressed one-line conditions, hidden side
  effects, and unnecessary abstractions.
- Keep production and test code readable, consistently formatted, and free of
  lines longer than 120 characters.
- Add or update focused tests for every behavioural change and relevant edge
  case.
- Run the complete test suite after structural changes.
