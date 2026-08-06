---
name: unit-test-report
description: This skill should be used when the user wants to run unit tests and produce a test report for the HouseDesign project, which has a Spring Boot backend (JUnit5 + Mockito + JaCoCo) and a Vue frontend (Vitest). It scaffolds missing test infrastructure, writes/runs backend and frontend unit tests, aggregates results into a CLI summary and a combined HTML report, and can watch source files to auto-run tests on change.
---

# Unit Test Report

Run unit tests across the HouseDesign project (Java backend + Vue frontend) and produce a
combined test report (CLI summary + HTML, with coverage for both stacks).

## When to use

- The user asks to "run unit tests", "test the code", "check coverage", or "generate a test report".
- The user wants tests written for a specific backend class or frontend component.
- The user wants tests to run automatically whenever source code changes.

## Project layout (assumed)

```
<root>/
  backend/   -> Spring Boot, Maven, JUnit5 + Mockito (spring-boot-starter-test), JaCoCo
  frontend/  -> Vue 3, Vite, Vitest (added by setup)
  .codebuddy/skills/unit-test-report/
```

## Workflow

### 1. Ensure test infrastructure exists

Run the scaffolder once (idempotent). It injects the JaCoCo Maven plugin into
`backend/pom.xml` and installs Vitest + coverage + test scripts + `vitest.config.js` for the
frontend. The frontend step requires network access on first run.

```bash
python .codebuddy/skills/unit-test-report/scripts/setup.py
```

### 2. Write tests when asked

When the user wants tests for specific code, follow the patterns in the references:

- Backend (Service / Controller): `references/junit-patterns.md`
- Frontend (util / component): `references/vitest-patterns.md`

Place backend tests under `backend/src/test/java/com/housedesign/...` mirroring `src/main`,
and frontend tests next to the code as `*.test.js` / `*.spec.js`.

### 3. Run tests and generate the report

```bash
python .codebuddy/skills/unit-test-report/scripts/run_tests.py
```

Options:
- `--backend-only` / `--frontend-only` : run only one stack.
- `--no-report` : print the CLI summary only, skip HTML/JSON.

This runs `mvn -q test` (backend) and `npm --prefix frontend run test:coverage` (frontend),
then:
- Prints a CLI summary with test counts, failures/errors, and coverage percentages.
- Writes a combined HTML report to
  `.codebuddy/skills/unit-test-report/reports/report-<timestamp>.html` (and `latest.html`).
- Writes machine-readable JSON to `reports/latest.json`.
- Links to the framework-native HTML reports: JaCoCo (`backend/target/site/jacoco/index.html`)
  and Vitest (`frontend/coverage/index.html`).

### 4. Auto-run on code change (watch mode)

To satisfy "run tests whenever code changes", start the watcher in the background:

```bash
python .codebuddy/skills/unit-test-report/scripts/watch.py
```

It polls `backend/src` and `frontend/src` every 2 seconds; on any change it debounces 3
seconds and re-invokes `run_tests.py`, refreshing `reports/latest.html`. Stop with Ctrl+C.

Alternatively, for recurring/scheduled execution, wrap `run_tests.py` in a CodeBuddy automation
that triggers on a cron or file-change basis.

## Notes

- Coverage thresholds are reported as badges: green >= 80%, yellow >= 60%, red < 60%.
- If `mvn test` reports nothing, confirm `setup.py` ran (JaCoCo) and that at least one
  `*.test.*` file exists for the frontend.
- All scripts are cross-platform Python 3 and resolve the project root relative to the skill
  directory, so they work without hard-coded paths.
