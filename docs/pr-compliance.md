# Pull Request Compliance & AI-Agent Governance Guide

This document defines the Pull Request description requirements and automated compliance gates for the **ingatlan-kereso** project.

---

## 1. Objectives & Core Principles

To keep Pull Requests concise, high-signal, and strictly aligned with repository quality standards, automated CI compliance gates enforce:

1. **Concise High-Signal Summaries:** Every PR focuses on the essentials: why the change was made, what changed, and which issue it closes.
2. **Fail-Closed Operation:** Any missing mandatory section, missing issue link, or unsynchronized presentation DTO / OpenAPI contract change causes the compliance gate to fail.
3. **Scope Containment:** Every modified file and diff segment must directly serve the target issue. Unsolicited refactoring or out-of-scope modifications are rejected.

---

## 2. Mandatory PR Description Structure (2 Sections)

Every Pull Request description must be written in **Hungarian** (clean UTF-8 without BOM) adhering strictly to the 2 mandatory sections from the [pull request template](../.github/pull_request_template.md):

### 2.1. 🎯 Miért készült? (Why was it created?)
A concise, 1–2 sentence summary explaining the business or technical motivation and context for the change.

### 2.2. 🛠️ Mit módosít / Mit javít? (What changes / What is fixed?)
A structured bulleted list highlighting key architectural, functional, and contractual modifications.

The PR description must conclude with an issue linking keyword: `Closes #<id>` or `Fixes #<id>`.

---

## 3. Automated Verification Tools & Workflows

### 3.1. Local & CI Compliance Validator (`verify-pr-compliance.mjs`)
The repository contains a standalone, zero-dependency compliance validator:
```bash
# Validate PR description and diff
node scripts/verify-pr-compliance.mjs --body-file pr_body.md --diff-base origin/develop

# Or via bash wrapper:
bash scripts/verify-pr-compliance.sh --body-file pr_body.md --diff-base origin/develop

# Run validator self-test suite:
node --test scripts/test-verify-pr-compliance.mjs
```

Key automated checks performed by `verify-pr-compliance.mjs`:
- Presence and non-empty content of the 2 mandatory sections (`🎯 Miért készült?`, `🛠️ Mit módosít / Mit javít?`).
- Issue closing reference (`Closes #<id>`, `Fixes #<id>`, or `Resolves #<id>`).
- Cross-Layer presentation DTO & OpenAPI contract drift check: modifying `presentation/*Dto.java` requires `openapi/api-spec.yaml` and `frontend/src/types/generated-api.ts` synchronization or explicit justification in the PR description.
- Automated exemptions: `dependabot[bot]` / `app/dependabot` PRs are exempt from the Hungarian 2-section body and issue-link checks, and `develop -> main` release PRs are exempt from the issue-link check.

### 3.2. Governance & Specialist Skill Validator (`validate-skills.sh`)
Deterministic offline validator enforcing 7 sequential phases:
```bash
bash scripts/validate-skills.sh -v
```

### 3.3. GitHub Actions PR Compliance Workflow (`.github/workflows/pr-compliance.yml`)
Runs automatically on PR open, edit (`edited`), or commit push:
- Validates the 2 mandatory sections, issue linking, and OpenAPI contract drift.
- Runs validator unit tests (`node --test scripts/test-verify-pr-compliance.mjs`).
- Executes `bash scripts/validate-skills.sh -v` unconditionally on every PR.

---

## 4. GitHub Merge Gate & Enforcement Hierarchy

Protection operates across two complementary levels:

### 4.1. Repository Versioned Workflow Gates (Always Active)
- **`CI Build`** (`.github/workflows/ci.yml`): Central fail-closed pipeline requiring backend tests (JaCoCo $\ge 80\%$), frontend lint/test steps, OpenAPI drift checks, Gitleaks secret scanning, and governance validation.
- **`PR Compliance & Agent Governance`** (`.github/workflows/pr-compliance.yml`): Description and governance compliance gate.
- **`Branch up-to-date with develop`** (`.github/workflows/pr-guards.yml`): Ensures branch is not out of date.
- **`PR title`** (`.github/workflows/pr-build.yml`): Enforces Conventional Commit PR titles.

### 4.2. GitHub Repository Settings (Administrator Scope)
Active rulesets protect default and integration branches:
1. `develop-1` (ID: 25079337): Protects `refs/heads/develop` against deletion and non-fast-forward pushes, activating automated Copilot reviews.
2. `Copilot review for default branch` (ID: 25079396): Protects the default branch.

Recommended Required Status Checks on GitHub Branch Protection / Rulesets:
- `CI Build`
- `PR Compliance & Agent Governance`
- `Branch up-to-date with develop`
- `PR title`
- `CodeQL`

---

## 5. Mandatory Adversarial Self-Review Protocol

Before opening any Pull Request or marking an implementation task complete, every AI agent must execute a two-phase workflow: first as an **Implementer**, then as an **Adversarial Reviewer** auditing their own diff against four mandatory invariant pillars:

1. **Multi-Source Domain Invariant:** Verify that deduplicated clusters (`IngatlanItem`) with multiple source listings are handled canonically. No UI component, hook, or filter may access `item.priceInfo.amountHuf` or `item.detailsInfo.pricePerSquareMeter` directly; canonical helpers `getEffectivePrice(item)` and `getEffectiveSqmPrice(item)` (`src/utils/formatters.ts`) must be used (enforced by ESLint AST rule `no-restricted-syntax`).
2. **Multi-City & Partial Failure Invariant:** Verify that search aggregation and portal cards properly support mixed statuses (`SUCCESS`, `PARTIAL_SUCCESS`, `TOTAL_FAILURE`, `BOT_PROTECTED`) across multiple queried cities without throwing unhandled exceptions or showing empty states.
3. **End-to-End Contract Traceability:** Verify that every backend DTO change propagates across `openapi/api-spec.yaml` -> `PresentationMapper` -> `generated-api.ts` -> UI components (enforced by `OpenApiSpecIntegrityTest.java` and `verify-pr-compliance.mjs`).
4. **Hermetic Test Isolation & Virtual Thread Concurrency:** Verify that tests run completely offline and order-independently without `app.test-mode` flags leaking into production code (`src/main/java`). Production code must contain zero `synchronized` methods or blocks to eliminate Java 25 virtual thread carrier pinning (enforced by Checkstyle `noSynchronized`, ArchUnit, and `validate-skills.sh`).
