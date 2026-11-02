---
name: quality-devops-engineer
description: Quality assurance, testing, containerization, and CI/CD automation engineer. Use this skill to design comprehensive test strategies; maintain deterministic offline JUnit 5 unit and integration tests, MockMvc and WireMock simulations, JaCoCo coverage checks (80%+), multi-stage Temurin JRE 25 Alpine containers, and GitHub Actions workflows and pull-request quality gates.
---

# Quality & DevOps Specialist

This specialist skill owns **software quality assurance**, **fully offline hermetic testing**, the **containerized environment (Docker and Compose)**, and **reliable CI/CD pipelines that eliminate false-green results**.

---

## 1. Purpose & Activation

### Primary Mission

Ensure every code change is covered by deterministic, network-independent tests; native Gradle `jacocoTestCoverageVerification` enforces a minimum 80% line coverage bound to `check`; Checkstyle reports zero violations under strict Clean Code metrics; frontend ESLint enforces a zero-warning gate (`--max-warnings 0`); the 6-part offline validator (`scripts/validate-skills.sh`) enforces architectural guardrails; containers meet security and health-check requirements; and CI/CD workflows prevent silent failures and false-green results.

### Activation Triggers

Activate this skill for any task, pull request, or code review affecting any of the following:

- **CI/CD workflows and actions**: `.github/workflows/**` (`ci.yml`, `dependency-submission.yml`, `pr-build.yml`, `release.yml`, `images.yml`)
- **PR template and verification scripts**: `.github/pull_request_template.md`, `.github/scripts/**` (`calculate-jacoco.sh`)
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
   - Use MockMvc endpoint tests, Spring Data JPA tests with embedded H2 or in-memory storage, and ArchUnit architecture rules (`ArchitectureRulesTest.java`).
   - Maintain the frontend Vitest and Playwright E2E test suites.
2. **Code quality gates & static analysis**:
   - Enforce minimum 80.00% line coverage via native Gradle `jacocoTestCoverageVerification` bound directly to `./gradlew check` across core and persistence packages (including MapStruct mappers `JpaEntityMapper` and `PresentationMapper`, with targeted null/fallback unit tests).
   - Coverage calculation (`calculate-jacoco.sh`) must fail closed: exit non-zero if the CSV is missing or coverage is below threshold.
   - Checkstyle 10.21.4 must report zero warnings (`maxWarnings = 0`, `isIgnoreFailures = false`) under strict Clean Code metrics: `MethodLength` <= 80 (non-empty), `ParameterNumber` <= 6, `CyclomaticComplexity` <= 12, `BooleanExpressionComplexity` <= 4, `NestedIfDepth` <= 3, `NestedTryDepth` <= 2, `DeclarationOrder`, and block forbidden Lombok annotations (`@Data`, `@EqualsAndHashCode`, `@ToString`, `@SneakyThrows`).
   - Frontend static analysis must enforce an ESLint zero-warning gate (`--max-warnings 0`) with Clean Code rules (`complexity: 15`, `max-depth: 4`, `max-params: 5`, `eqeqeq`, `@typescript-eslint/consistent-type-imports`, `@typescript-eslint/no-explicit-any: error`) and AST restriction against direct `window.open`.
   - Maintain the 6-part deterministic offline validator (`scripts/validate-skills.sh`) enforcing YAML frontmatter, 8 mandatory sections, decision tokens, relative links, banned words, and architectural guardrails / frontend OpenAPI derivation.
   - MapStruct mappers must compile with `unmappedTargetPolicy = ReportingPolicy.ERROR`, and frontend OpenAPI types (`npm run generate-api`) must stay 100% synchronized with `openapi/api-spec.yaml` with zero dead DTOs or unused API client functions.
3. **Containerization and release security**:
   - Multi-stage Temurin JRE 25 runtime container; run as non-root (`appuser:appuser`, UID 10001), use declared volumes (`/app/data`, `/app/logs`), and enforce a health check (`curl -f http://localhost:8080/api/status`).
   - Docker Compose three-tier architecture: chain health dependencies (`service_healthy`) for `db` (PostgreSQL 17), `backend`, and `frontend` (Nginx SPA).
   - Maintain smoke-test automation in `scripts/smoke-test-docker-compose.sh`.
   - Use numeric SemVer versions from `build.gradle.kts`; Git and GitHub release tags use the `vX.Y.Z` prefix, while release image tags use `X.Y.Z` and `sha-<commit>`.
4. **CI/CD pipeline reliability (eliminating false-green results)**:
   - Comprehensive path filtering (`dorny/paths-filter`) must ensure Docker, Gradle, scripts, configuration, and AI directive changes cannot bypass CI.
   - Minimal fail-closed gate: any required job failure, cancellation, or unexpected skip fails the workflow without producing a verbose summary report.
   - Require a Conventional Commit PR title.
   - Run the shared CI checks on the selected commit before manual release or `develop` image publication.
   - Keep release tags and GHCR write permissions scoped to the publishing jobs.
5. **Issue-scope CI verification and anti-scope-creep quality gates**:
   - Verify that all changes in the PR diff map directly to the issue's requirements and acceptance criteria in the traceability table.
   - Guard against unrequested technology version bumps, package moves, or opportunistic refactors in pull requests.

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
   - Check path filters, required job dependencies, the minimal fail-closed gate, JaCoCo thresholds, Checkstyle, and Docker security configuration.
3. **Phase 3: Deterministic local verification**
   - Run backend check suite: `./gradlew checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification check bootJar`.
   - Run offline skills and architecture validator: `bash scripts/validate-skills.sh -v` (all 6 checks must pass).
   - Run frontend check suite: `cd frontend && npm run generate-api && npm run typecheck && npm run lint && npm run test && npm run build` (ESLint zero-warning gate `--max-warnings 0`).
   - Run Docker smoke tests when container configuration is affected.
   - Verify that standard unit and integration tests do not require live external network access.
4. **Phase 4: Synthesis and decision**
   - Determine the decision token (`NO_FINDING`, `FINDING`, `NOT_APPLICABLE`, or `NOT_VERIFIED`).
   - Record a concise rationale for the review decision.

---

## 4. Review Checklist

### A. Hermetic Testing and Test Determinism

- [ ] `./gradlew check` passes deterministically without network access.
- [ ] Live portal tests are explicitly tagged and excluded from normal test runs.
- [ ] Unit and integration tests use MockMvc, WireMock, and embedded or in-memory persistence where appropriate.
- [ ] ArchUnit JUnit 5 suite (`ArchitectureRulesTest.java`) passes with zero violations.
- [ ] Frontend Vitest and Playwright E2E coverage is maintained.
- [ ] Regression Test First protocol is verified for business logic modifications (`docs/business-logic-regression.md`): failing test cases prove defects prior to fixes, and regression suites run deterministically offline.

### B. Code Quality Gates

- [ ] JaCoCo line coverage is at least 80.00% enforced via native Gradle `jacocoTestCoverageVerification` bound directly to `./gradlew check` for core and persistence packages (including MapStruct mapper null/fallback branches).
- [ ] Coverage calculation fails closed when the CSV is missing or the threshold is not met.
- [ ] Checkstyle 10.21.4 reports zero warnings with `maxWarnings = 0` and `isIgnoreFailures = false`, enforcing Clean Code metrics (`MethodLength` <= 80, `ParameterNumber` <= 6, `CyclomaticComplexity` <= 12, `BooleanExpressionComplexity` <= 4, `NestedIfDepth` <= 3, `NestedTryDepth` <= 2, `DeclarationOrder`) and blocking forbidden Lombok annotations (`@Data`, `@EqualsAndHashCode`, `@ToString`, `@SneakyThrows`).
- [ ] Frontend `npm run generate-api`, `npm run lint`, `npm run typecheck`, `npm run test`, and `npm run build` pass without warnings (`--max-warnings 0`), without hand-duplicated OpenAPI DTOs, and with zero direct `window.open` calls.
- [ ] Offline validator `scripts/validate-skills.sh -v` passes all 6 checks cleanly.

### C. Containerization and Release Security

- [ ] The runtime container uses a non-root user (`appuser`, UID 10001).
- [ ] Docker Compose coordinates PostgreSQL, backend, and frontend health checks using `service_healthy`.
- [ ] Health checks validate the actual application status endpoint.
- [ ] Docker image tags follow clean numeric SemVer without a `v` prefix.

### D. CI/CD Reliability

- [ ] Path filters include build files, scripts, tests, Docker, configuration, and AI instruction changes.
- [ ] The minimal CI gate fails if any required job fails or is skipped unexpectedly.
- [ ] The PR build workflow blocks titles that do not follow Conventional Commits.
- [ ] Docker publishing is gated on successful CI results.
- [ ] PR changes map directly to issue acceptance criteria: no unsolicited refactoring or out-of-scope diffs.
- [ ] Automated architecture regression gates (ArchUnit and negative tests) pass locally and in CI.

---

## 5. Evidence & Output

The specialist must record the following concrete evidence during review:

- **Test evidence**: Relevant `./gradlew check` and frontend test results.
- **Coverage evidence**: JaCoCo report and the result of `calculate-jacoco.sh` / `jacocoTestCoverageVerification`.
- **CI/CD and container evidence**: Workflow status, path-filter behavior, health checks, and Docker runtime user.

### Thematic Agent Approval Gate Output

Use the following text in the Pull Request's Thematic Gate block:

```markdown
- [x] Quality & DevOps (`quality-devops-engineer`): [NO_FINDING / NOT_APPLICABLE] - [One concise sentence summarizing testing, coverage, Checkstyle, and CI/CD integrity]
```

*Note: For `FINDING`, leave the checkbox unchecked (`[ ]`), set the status to `[FINDING]`, and attach a detailed report in the PR description or a comment.*

---

## 6. Standard Decision Semantics

- **`FINDING`**: Use when offline tests access the network, coverage falls below 80%, Checkstyle reports violations, ESLint produces warnings under `--max-warnings 0`, containers run as root, or a CI false-green gap is found.
  - *Required format*: `FINDING: [Severity: CRITICAL/MAJOR/MINOR] - [File:Line] - [Description and remediation guidance]`
- **`NO_FINDING`**: All required tests pass, coverage is at least 80%, Checkstyle is clean under Clean Code rules, ESLint has zero warnings, and container and CI configurations are robust and false-green resistant.
- **`NOT_APPLICABLE`**: The task only affects user documentation or isolated frontend styling without changing tests, build, container, or CI configuration.
- **`NOT_VERIFIED`**: Tests or coverage calculations cannot be run (for example, due to a broken build environment). As a fail-closed safeguard, the PR must not be approved.

---

## 7. Stop Conditions

The specialist must stop immediately and reject approval with a `FINDING` status if any of the following conditions occurs:

1. **Live network access during tests**: `./gradlew test` or `check` attempts to reach external endpoints instead of using mocks.
2. **Coverage regression**: JaCoCo line coverage falls below 80.00% in core or persistence packages, or `jacocoTestCoverageVerification` fails.
3. **False-green behavior**: A CI step or script masks a failing exit code (`continue-on-error: true`, `|| true`) or treats a missing report as success.
4. **Checkstyle violation**: Any formatting or Clean Code convention violation remains when `maxWarnings = 0`.
5. **Frontend lint or type regression**: ESLint reports any warning under `--max-warnings 0`, or TypeScript strict compiler options fail.
6. **Skills or architecture validator failure**: Any check fails in `scripts/validate-skills.sh`.
7. **Root container**: The Docker runtime stage does not run as `appuser` with UID 10001.
8. **Unverified Docker publishing**: Docker images are built or published without successful CI confirmation.
9. **PR title bypass**: The required PR build check accepts a title that does not follow Conventional Commits.

---

## 8. Completion Criteria

Approval (`NO_FINDING`) from `quality-devops-engineer` may be granted only when ALL of the following criteria are met:

1. `./gradlew checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification check bootJar` passes offline with zero test failures, zero Checkstyle Clean Code warnings, and >=80.00% line coverage.
2. Frontend verification (`npm run generate-api && npm run typecheck && npm run lint && npm run test && npm run build`) passes with zero errors and zero warnings (`--max-warnings 0`).
3. The offline `validate-skills.sh` validator reports zero errors across all 6 checks.
4. Docker and Compose configuration meets container-security and health-check requirements.
5. CI/CD path filters and the minimal gate prevent false-green results.
6. The PR build workflow requires a valid Conventional Commit title.
