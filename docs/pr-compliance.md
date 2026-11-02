# Pull Request Compliance & AI-Agent Governance Guide

This document defines the quality assurance, acceptance criteria traceability, and governance requirements for Pull Requests submitted by AI agents and developers in the **ingatlan-kereso** project.

---

## 1. Objectives & Core Principles

To ensure that changes authored by AI agents (e.g., Google Antigravity, GitHub Copilot) remain reliable, traceable, and strictly aligned with repository quality standards, automated CI compliance gates enforce the following core principles:

1. **Evidence-Based Verification:** Statements in PR descriptions are insufficient on their own; every claim must be backed by executed test commands, code references, and diff analysis.
2. **Risk-Driven Review Obligation:** Review requirements are determined by the modified files and their risk profile per the Review Risk Matrix.
3. **Fail-Closed Operation:** Any missing mandatory section, unverified status (`[NOT_VERIFIED]`), or unresolved issue (`[FINDING]`) immediately causes the compliance gate to fail.
4. **Traceability & Scope Containment:** Every modified file and diff segment must directly trace back to the target issue's acceptance criteria. Unsolicited refactoring or out-of-scope modifications are rejected.

---

## 2. Mandatory PR Description Structure (5 Sections)

Every Pull Request description must be written in **Hungarian** (clean UTF-8 without BOM) adhering strictly to the 5 mandatory sections from the [pull request template](../.github/pull_request_template.md):

### 2.1. 🎯 Miért készült? (Why was it created?)
A concise, 1–2 sentence summary explaining the business or technical motivation and context for the change.

### 2.2. 🛠️ Mit módosít / Mit javít? (What changes / What is fixed?)
A structured bulleted list highlighting key architectural, functional, and contractual modifications.

### 2.3. 📋 Acceptance Criteria Traceability
Every issue acceptance criterion must be mapped to implementation file paths and concrete validation evidence. If a criterion is not applicable, explicit justification is required.

Format (Markdown table or structured list):
```markdown
| Issue Acceptance Criterion | Implementation (Files / Components) | Test / Validation Evidence |
| :--- | :--- | :--- |
| Criterion 1 description | `src/.../MyService.java` | `MyServiceTest#testFeature` (JaCoCo: 88%) |
| Criterion 2 description | `frontend/src/.../MyView.tsx` | `npm run test` (Vitest PASS) |
```

The Traceability table must account for all modified files in the diff. Unsolicited refactoring, unrequested version bumps, or unapproved contract changes will cause the compliance gate to reject the PR. Small supportive edits are permitted only with explicit justification in the table.

### 2.4. 🧪 Tesztelés és Ellenőrzés (Testing & Verification)
The exact verification commands executed locally and their outcomes (e.g., `./gradlew check`, `npm run lint`, `bash scripts/validate-skills.sh -v`).

### 2.5. 🛡️ Thematic Agent Approval Gate
Formal sign-off from designated specialist personas using standardized decision tokens:
```markdown
- [x] **fullstack-architect**: [NO_FINDING] - Architectural integrity and type safety verified.
- [x] **scraper-security-engineer**: [NOT_APPLICABLE] - Does not touch portal scraper or network logic.
- [x] **quality-devops-engineer**: [NO_FINDING] - Test coverage (>=80%), Checkstyle, and CI/CD gates satisfied.
```

The PR description must conclude with an issue linking keyword: `Closes #<id>` or `Fixes #<id>`.

---

## 3. Standard Decision Status Tokens

The Thematic Gate block exclusively permits the following four standardized decision tokens:

| Token | Meaning | CI Gate Effect | Required Content |
| :--- | :--- | :--- | :--- |
| **`[NO_FINDING]`** | Review passed with no defects found | **PASS** | Exactly 1 concise sentence summarizing verified invariants |
| **`[NOT_APPLICABLE]`** | Change does not touch specialist's domain | **PASS** (if diff confirms) | Mandatory 1-sentence rationale explaining why not relevant |
| **`[FINDING]`** | Violation or defect identified | **FAIL (Blocking)** | Defect description and proposed remediation |
| **`[NOT_VERIFIED]`** | Unverified or unable to run checks | **FAIL (Blocking)** | Not acceptable as completed work |

> [!CAUTION]
> Legacy or informal status markers (such as `[OK]`, `[NOK]`, `[PASS]`, `[N/A]`) are **strictly forbidden**; using them triggers immediate CI failure.

---

## 4. Review Risk Matrix (Scope Verification)

Based on the paths of modified files in the PR diff, the CI compliance engine automatically verifies whether the required specialist sign-offs are present. If a domain's files are modified, the corresponding specialist **cannot** be marked as `[NOT_APPLICABLE]`:

| Change Type | Affected File Path Patterns | Mandatory Specialist | Expected Validation |
| :--- | :--- | :--- | :--- |
| **Backend business logic & models** | `src/main/java/ingatlanok/core/**`<br>`src/main/java/ingatlanok/model/**` | `fullstack-architect`<br>`quality-devops-engineer` | Unit & integration tests, JaCoCo $\ge 80\%$, Checkstyle Clean Code |
| **API & OpenAPI contract** | `openapi/**`<br>`src/main/java/ingatlanok/web/**`<br>`src/main/java/ingatlanok/presentation/**`<br>`frontend/src/types/generated-api.ts` | `fullstack-architect` | OpenAPI drift check (`npm run generate-api`), DTO immutability |
| **DB & Persistence** | `src/main/java/ingatlanok/persistence/**` | `fullstack-architect` | Spring Data JPA tests, storage boundary isolation (no direct SQL in core) |
| **Scraper, parser & network I/O** | `src/main/java/ingatlanok/portal/**`<br>`src/main/java/ingatlanok/common/HttpFetcher.java`<br>`src/main/java/ingatlanok/common/PropertyParser.java`<br>`tools/scrapers/**`<br>`tools/sidecar*` | `scraper-security-engineer` | 0-crash guarantee, ReDoS prevention, SSRF isolation (`HttpFetcher`), 15s timeout |
| **Frontend UI, state & client** | `frontend/**` (except `generated-api.ts`) | `fullstack-architect` | Strict TypeScript check (`noImplicitReturns`), ESLint (`--max-warnings 0`), Vitest, Playwright |
| **CI/CD, DevOps, Governance** | `.github/**`<br>`.agents/**`<br>`scripts/**`<br>`AGENTS.md`<br>`GEMINI.md`<br>`gemini.md`<br>`CONTRIBUTING.md`<br>`docs/**`<br>`Dockerfile*`<br>`docker-compose.yml`<br>`config/checkstyle/**` | `quality-devops-engineer` | `validate-skills.sh`, PR compliance unit tests, container security, fail-closed CI gate |
| **Architecture & module boundaries** | `package-info.java`<br>`build.gradle.kts` dependencies | `fullstack-architect` | ArchUnit tests (`ArchitectureRulesTest.java`), fail-closed javac (`-Werror`) |

---

## 5. Automated Verification Tools & Workflows

### 5.1. Local & CI Compliance Validator (`verify-pr-compliance.mjs`)
The repository contains a standalone, zero-dependency compliance validator:
```bash
# Validate PR description and diff
node scripts/verify-pr-compliance.mjs --body-file pr_body.md --diff-base origin/develop

# Or via bash wrapper:
bash scripts/verify-pr-compliance.sh --body-file pr_body.md --diff-base origin/develop

# Run validator self-test suite (14 unit tests):
node --test scripts/test-verify-pr-compliance.mjs
```

### 5.2. Governance & Specialist Skill Validator (`validate-skills.sh`)
Deterministic offline validator enforcing 7 sequential phases:
```bash
bash scripts/validate-skills.sh -v
```

### 5.3. GitHub Actions PR Compliance Workflow (`.github/workflows/pr-compliance.yml`)
Runs automatically on PR open, edit (`edited`), or commit push:
- Validates the 5 mandatory sections and the Traceability table.
- Analyzes modified files against the Review Risk Matrix.
- Runs validator unit tests.
- Executes `validate-skills.sh` if governance or documentation files changed.

---

## 6. GitHub Merge Gate & Enforcement Hierarchy

Protection operates across two complementary levels:

### 6.1. Repository Versioned Workflow Gates (Always Active)
- **`CI Build`** (`.github/workflows/ci.yml`): Central fail-closed pipeline requiring backend tests (JaCoCo $\ge 80\%$), frontend lint/test steps, OpenAPI drift checks, Gitleaks secret scanning, and governance validation.
- **`PR Compliance & Agent Governance`** (`.github/workflows/pr-compliance.yml`): Description and governance compliance gate.
- **`Branch up-to-date with develop`** (`.github/workflows/pr-guards.yml`): Ensures branch is not out of date.
- **`PR title`** (`.github/workflows/pr-build.yml`): Enforces Conventional Commit PR titles.

### 6.2. GitHub Repository Settings (Administrator Scope)
Active rulesets protect default and integration branches:
1. `develop-1` (ID: 25079337): Protects `refs/heads/develop` against deletion and non-fast-forward pushes, activating automated Copilot reviews.
2. `Copilot review for default branch` (ID: 25079396): Protects the default branch.

Recommended Required Status Checks on GitHub Branch Protection / Rulesets:
- `CI Build`
- `PR Compliance & Agent Governance`
- `Branch up-to-date with develop`
- `PR title`
- `CodeQL`
