---
name: testing-after-code
description: "Require tests after every code change. Use when adding, modifying, refactoring, or deleting source code, tests, configuration that affects runtime behavior, or build logic. Run the smallest relevant test immediately after each coherent code change, report failures explicitly, and run the complete project test suite before declaring the task complete."
argument-hint: "Optional: describe the code change to validate"
user-invocable: true
disable-model-invocation: false
---

# Testing After Code Changes

Apply this workflow whenever code or behavior changes.

## Required workflow

1. Inspect the repository before editing:
   - Identify the language, build tool, test framework, and existing test commands.
   - Check for project-specific instructions and existing tests related to the code being changed.
2. Make one coherent, focused code change. Keep unrelated edits separate.
3. Immediately run the smallest relevant automated test after that change:
   - Prefer a directly related test class, test file, or test case.
   - If no focused test exists, run the narrowest available module/package test.
   - For this Maven project, use `mvn -q -Dtest=<TestClass> test` for a focused test when possible.
4. If the focused test fails:
   - Stop making additional functional changes.
   - Read the complete failure output and diagnose the root cause.
   - Fix the implementation or test, then rerun the focused test.
   - Do not report success based on compilation alone.
5. Repeat steps 2–4 for every subsequent coherent code change. Do not batch multiple unvalidated code changes merely to reduce test runs.
6. Before completion, run the full project test suite:
   - Maven: `mvn test`
   - Gradle: `gradlew test` or `./gradlew test`
   - Node.js: the repository's test script, such as `npm test`
   - Python: the repository's configured test command, such as `pytest`
7. Report validation in the final response:
   - Commands run
   - Whether each command passed, failed, or was not runnable
   - Any known limitations, skipped tests, warnings, or pre-existing failures

## Test selection rules

- Reuse the project's existing test framework and scripts; do not add a new test tool solely to satisfy this workflow.
- Add or update tests when behavior changes and existing coverage does not exercise the new behavior.
- For bug fixes, first add a regression test when practical, then implement the fix and run that test.
- For changes to public APIs, controllers, persistence, security, or configuration, include an appropriate integration or contract-level test when unit tests cannot validate the behavior.
- If the change is test-only, run the affected tests and then the full suite before completion.
- Documentation-only changes do not require tests unless documentation tests or validation scripts exist.

## Failure and environment rules

- Never claim that tests passed without running them.
- Never replace a failed test with a weaker command unless the failure is caused by a documented environment limitation; if so, state the limitation and run the strongest available validation.
- Do not silently ignore compilation errors, test failures, flaky-test retries, or skipped tests.
- Install or restore dependencies only when the repository manifest changed or the selected test command reports a missing dependency.
- Keep temporary test artifacts out of the repository and clean up artifacts created solely for validation.

## Completion checklist

- [ ] Every coherent code change was followed by a focused test or the narrowest applicable validation.
- [ ] New or changed behavior has automated coverage, or the final response explains why coverage is not practical.
- [ ] The full project test suite was run after the final code change.
- [ ] Validation results and limitations are reported accurately.
