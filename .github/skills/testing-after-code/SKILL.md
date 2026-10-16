---
name: testing-after-code
description: "Require tests and test-case documentation after every code change. Use when adding, modifying, refactoring, or deleting source code, tests, configuration that affects runtime behavior, or build logic. Run the smallest relevant test immediately after each coherent code change, document each test as it is created or changed, and run the complete project test suite before declaring the task complete."
argument-hint: "Optional: describe the code change to validate"
user-invocable: true
disable-model-invocation: false
---

# Testing After Code Changes

Apply this workflow whenever code or behavior changes.

## Required workflow

1. Inspect the repository before editing:
   - Identify the language, build tool, test framework, and existing test commands.
   - Check for project-specific instructions, existing tests, and the test
     documentation catalog.
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
5. Repeat steps 2–4 for every subsequent coherent code change. Do not batch
   multiple unvalidated code changes merely to reduce test runs.
6. Document every test as it is created or changed:
   - Add or update one detailed case file in `docs/Test/cases/`.
   - Add or update its brief row in `docs/Test/test-case-catalog.md`.
   - Include the test ID, purpose, setup, steps, expected result, source link,
     and important limitations or review notes.
   - Keep the catalog brief; put procedural detail in the individual case
     file.
   - Keep documentation synchronized with the test in the same change.
7. Before completion, run the full project test suite:
   - Maven: `mvn test`
   - Gradle: `gradlew test` or `./gradlew test`
   - Node.js: the repository's test script, such as `npm test`
   - Python: the repository's configured test command, such as `pytest`
8. Report validation in the final response:
   - Commands run
   - Whether each command passed, failed, or was not runnable
   - Any known limitations, skipped tests, warnings, or pre-existing failures
   - Test documentation files created or updated

## Test selection and documentation rules

- Reuse the project's existing test framework and scripts; do not add a new
  test tool solely to satisfy this workflow.
- Add or update tests when behavior changes and existing coverage does not
  exercise the new behavior.
- For bug fixes, first add a regression test when practical, document it, then
  implement the fix and run that test.
- For changes to public APIs, controllers, persistence, security, or
  configuration, include an appropriate integration or contract-level test
  when unit tests cannot validate the behavior.
- If the change is test-only, run the affected tests and update the relevant
  test documentation before running the full suite.
- Documentation-only changes do not require tests unless documentation tests or
  validation scripts exist.
- If a test is removed or renamed, update the catalog and remove or clearly
  mark its detailed case file so the catalog does not describe nonexistent
  coverage.

## Failure and environment rules

- Never claim that tests passed without running them.
- Never replace a failed test with a weaker command unless the failure is caused
  by a documented environment limitation; if so, state the limitation and run
  the strongest available validation.
- Do not silently ignore compilation errors, test failures, flaky-test retries,
  or skipped tests.
- Install or restore dependencies only when the repository manifest changed or
  the selected test command reports a missing dependency.
- Keep temporary test artifacts out of the repository and clean up artifacts
  created solely for validation.

## Completion checklist

- [ ] Every coherent code change was followed by a focused test or the
  narrowest applicable validation.
- [ ] New or changed behavior has automated coverage, or the final response
  explains why coverage is not practical.
- [ ] Every created or changed test is documented in `docs/Test/`.
- [ ] The catalog remains brief and links to the detailed case documents.
- [ ] The full project test suite was run after the final code change.
- [ ] Validation results and limitations are reported accurately.
