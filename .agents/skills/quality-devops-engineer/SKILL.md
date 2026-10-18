---
name: quality-devops-engineer
description: Quality assurance, testing, containerization, and CI/CD automation engineer. Use this skill to design comprehensive test strategies; maintain deterministic offline JUnit 5 unit and integration tests, MockMvc and WireMock simulations, JaCoCo coverage checks (80%+), multi-stage Temurin JRE 21 and GraalVM Native Image containers, and GitHub Actions workflows and pull-request quality gates.
---

# Quality & DevOps Specialist

This specialist skill owns **software quality assurance**, **fully offline hermetic testing**, the **containerized environment (Docker and Compose)**, and **reliable CI/CD pipelines that eliminate false-green results**.

---

## 1. Purpose & Activation

### Primary Mission

Ensure every code change is covered by deterministic, network-independent tests; JaCoCo coverage never falls below the 80% threshold; Checkstyle reports zero violations; containers meet security and health-check requirements; and CI/CD workflows prevent silent failures and false-green results.

### Activation Triggers

Activate this skill for any task, pull request, or code review affecting any of the following:

- **CI/CD workflows and actions**: `.github/workflows/**` (`ci.yml`, `pr-review.yml`, `docker-publish.yml`)
- **PR template and verification scripts**: `.github/pull_request_template.md`, `.github/scripts/**` (`verify-pr-gate.sh`, `calculate-jacoco.sh`)
- **Containerization configuration**: `Dockerfile*`, `docker-compose.yml`, `frontend/Dockerfile`, `frontend/nginx.conf`
- **Automation and runtime scripts**: `scripts/**` (`validate-skills.sh`, `smoke-test-docker-compose.sh`, `start-docker.*`)
- **Backend and frontend test files**: `src/test/**`, `frontend/**/__tests__/**`, `frontend/e2e/**`
- **Quality assurance configuration**: `config/checkstyle/**`, `build.gradle.kts` (test/JaCoCo/Checkstyle blocks)
- **AI directives and policies**: `GEMINI.md`, `gemini.md`, `.agents/**`
- **PR labels**: `ci`, `devops`, `testing`, `quality`

---

## 2. Scope & Non-Goals

### In-Scope Responsibilities

1. **Fully offline hermetic testing (QA)**:
   - `./gradlew check` must run deterministically without network access.
   - Keep live portal network tests separate using `@Tag("live")` and `excludeTags("live")` in the normal test cycle; use a dedicated `liveTest` task.
   - Use MockMvc endpoint tests and Spring Data JPA tests with embedded H2 or in-memory storage.
   - Maintain the frontend Vitest and Playwright E2E test suites.
2. **Code quality gates**:
   - Enforce a minimum of 80% JaCoCo instruction and line coverage in core and persistence packages.
   - Coverage calculation (`calculate-jacoco.sh`) must fail closed: exit non-zero if the CSV is missing or coverage is below threshold.
   - Checkstyle 10.21.4 must report zero warnings (`maxWarnings = 0`, `isIgnoreFailures = false`).
3. **Containerization and release security**:
   - Multi-stage Temurin JRE 21 runtime container; run as non-root (`appuser:appuser`, UID 10001), use declared volumes (`/app/data`, `/app/logs`), and enforce a health check (`curl -f http://localhost:8080/api/status`).
   - Docker Compose three-tier architecture: chain health dependencies (`service_healthy`) for `db` (PostgreSQL 17), `backend`, and `frontend` (Nginx SPA).
   - Maintain smoke-test automation in `scripts/smoke-test-docker-compose.sh`.
   - Clean numeric SemVer: Docker tags and release versions must strictly use `1.0.0` format without a `v` prefix.
4. **CI/CD pipeline reliability (eliminating false-green results)**:
   - Comprehensive path filtering (`dorny/paths-filter`) must ensure Docker, Gradle, scripts, configuration, and AI directive changes cannot bypass CI.
   - Fail-closed summary job (`ci-summary`): any required job failure or omission must fail the pipeline.
   - PR gate verification (`verify-pr-gate.sh`): fail closed on diff errors and validate statuses and rationales.
   - Publish Docker images only after CI succeeds, using `workflow_run`.

### Explicit Non-Goals

- **Business logic and React UI development**: Entity modeling, API endpoints, and UI components belong to `fullstack-architect`.
- **Scraper DOM analysis and anti-bot mechanisms**: Jsoup selector tuning and bot-protection detection belong to `scraper-security-engineer`.

---

## 3. Required Workflow

For every review, the specialist must follow this four-phase workflow:

```
┌─────────────────────────────────┐
│ 1. Scope & File Audit           │ ➔ CI/CD, Docker, scripts, and tests
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 2. Pipeline & Quality Audit     │ ➔ False-green gaps, coverage, Checkstyle, Docker
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 3. Deterministic Local Checks   │ ➔ ./gradlew check, calculate-jacoco, smoke tests
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 4. Synthesis & Decision         │ ➔ Thematic gate status (NO_FINDING / FINDING)
└─────────────────────────────────┘
```

1. **Phase 1: Scope and file audit**
   - Inspect the changed file list.
   - Identify whether the change affects workflows (`.github/workflows/`), Docker, scripts, or test files.
2. **Phase 2: Pipeline and quality audit**
   - Check path filters, required job dependencies, the summary gate, JaCoCo thresholds, Checkstyle, and Docker security configuration.
3. **Phase 3: Deterministic local verification**
   - Run `./gradlew check` and the repository's skill validator.
   - Run relevant coverage scripts and Docker smoke tests when affected and available.
   - Verify that standard unit tests do not require live external network access.
4. **Phase 4: Synthesis and decision**
   - Determine the decision token (`NO_FINDING`, `FINDING`, `NOT_APPLICABLE`, or `NOT_VERIFIED`).
   - Provide exactly one concise, professional rationale sentence for the PR gate.

---

## 4. Review Checklist

### A. Hermetic Testing and Test Determinism

- [ ] `./gradlew check` passes deterministically without network access.
- [ ] Live portal tests are explicitly tagged and excluded from normal test runs.
- [ ] Unit and integration tests use MockMvc, WireMock, and embedded or in-memory persistence where appropriate.
- [ ] Frontend Vitest and Playwright E2E coverage is maintained.

### B. Code Quality Gates

- [ ] JaCoCo instruction and line coverage are at least 80% for core and persistence packages.
- [ ] Coverage calculation fails closed when the CSV is missing or the threshold is not met.
- [ ] Checkstyle 10.21.4 reports zero warnings with `maxWarnings = 0` and `isIgnoreFailures = false`.

### C. Containerization and Release Security

- [ ] The runtime container uses a non-root user (`appuser`, UID 10001).
- [ ] Docker Compose coordinates PostgreSQL, backend, and frontend health checks using `service_healthy`.
- [ ] Health checks validate the actual application status endpoint.
- [ ] Docker image tags follow clean numeric SemVer without a `v` prefix.

### D. CI/CD Reliability

- [ ] Path filters include build files, scripts, tests, Docker, configuration, and AI instruction changes.
- [ ] The summary job fails if any required job fails or is skipped unexpectedly.
- [ ] PR gate validation fails closed for malformed diffs, missing status tokens, or empty rationales.
- [ ] Docker publishing is gated on successful CI results.

---

## 5. Evidence & Output

The specialist must record the following concrete evidence during review:

- **Test evidence**: Relevant `./gradlew check` and frontend test results.
- **Coverage evidence**: JaCoCo report and the result of `calculate-jacoco.sh`.
- **CI/CD and container evidence**: Workflow status, path-filter behavior, health checks, and Docker runtime user.

### Thematic Agent Approval Gate Output

Use the following text in the Pull Request's Thematic Gate block:

```markdown
- [x] Quality & DevOps (`quality-devops-engineer`): [NO_FINDING / NOT_APPLICABLE] - [One concise sentence summarizing test, coverage, container, and CI quality]
```

*Note: For `FINDING`, leave the checkbox unchecked (`[ ]`), set the status to `[FINDING]`, and attach a detailed report in the PR description or a comment.*

---

## 6. Standard Decision Semantics

- **`FINDING`**: Use when offline tests access the network, coverage falls below 80%, Checkstyle reports violations, containers run as root, or a CI false-green gap is found.
  - *Required format*: `FINDING: [Severity: CRITICAL/MAJOR/MINOR] - [File:Line] - [Description and remediation guidance]`
- **`NO_FINDING`**: All required tests pass, coverage is at least 80%, Checkstyle is clean, and container and CI configurations are robust and false-green resistant.
- **`NOT_APPLICABLE`**: The task only affects user documentation or isolated frontend styling without changing tests, build, container, or CI configuration.
- **`NOT_VERIFIED`**: Tests or coverage calculations cannot be run (for example, due to a broken build environment). As a fail-closed safeguard, the PR must not be approved.

---

## 7. Stop Conditions

The specialist must stop immediately and reject approval with a `FINDING` status if any of the following conditions occurs:

1. **Live network access during tests**: `./gradlew test` or `check` attempts to reach external endpoints instead of using mocks.
2. **Coverage regression**: JaCoCo coverage falls below 80% in core or persistence packages.
3. **False-green behavior**: A CI step or script masks a failing exit code (`continue-on-error: true`, `|| true`) or treats a missing report as success.
4. **Checkstyle violation**: Any formatting or convention violation remains when `maxWarnings = 0`.
5. **Root container**: The Docker runtime stage does not run as `appuser` with UID 10001.
6. **Unverified Docker publishing**: Docker images are built or published without successful CI confirmation.
7. **PR gate bypass**: The Thematic Approval Gate contains an empty rationale or an unsupported status token.

---

## 8. Completion Criteria

Approval (`NO_FINDING`) from `quality-devops-engineer` may be granted only when ALL of the following criteria are met:

1. `./gradlew check` passes offline with zero test failures and zero Checkstyle warnings.
2. JaCoCo coverage is at least 80%, and `calculate-jacoco.sh` exits successfully.
3. The offline `validate-skills.sh` validator reports zero errors.
4. Docker and Compose configuration meets container-security and health-check requirements.
5. CI/CD path filters and summary statuses prevent false-green results.
6. The PR Gate block contains exactly one professional, non-generic rationale sentence.
