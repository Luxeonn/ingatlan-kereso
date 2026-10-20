# Contributing Guide

This document covers the internal development workflow for **ingatlan-kereso** — branch strategy, Git worktrees, CI pipeline checks, issue tracking, and AppSec/CodeQL requirements.

---

## 1. Branch Strategy & Git Worktrees

**Direct pushes to `main` or `develop` are strictly prohibited.** Every change ships through a dedicated feature branch.

### Create a Branch via Git Worktree

Always isolate work in a separate worktree to keep the working directory clean:

```bash
git worktree add -b <branch-name> ../worktrees/<branch-name> develop
```

### Branch Naming Convention

All branches must start from `develop` and follow one of these prefixes:

| Prefix | Use for |
| :--- | :--- |
| `feature/<short-desc>` | New functionality |
| `fix/<short-desc>` | Bug fixes |
| `refactor/<short-desc>` | Code restructuring without functional change |
| `docs/<short-desc>` | Documentation-only updates |
| `chore/<short-desc>` | Build system, dependency, or tooling changes |

---

## 2. Commit Messages

Use **Conventional Commits** format for every commit. The subject must be in English.

```
<type>(<scope>): <short imperative description>
```

**Examples:**

```
feat(search): add area filter to search criteria
fix(portal): handle missing price on Jófogás listings
refactor(dedup): extract O(1) key generation to helper
docs: update architecture diagram in docs/architecture.md
chore(deps): bump Spring Boot to 4.1.2
```

Commit each logical increment separately. Do not squash unrelated changes and do not publish trial-and-error debug commits to the remote branch — fix locally first, then push once.

---

## 3. CI Pipeline & Checks

Every pull request must pass **all** required CI checks before it is considered mergeable.

### Required CI Workflows

| Workflow | Trigger | What it checks |
| :--- | :--- | :--- |
| `ci.yml` — **CI Build** | Push / PR | Backend Checkstyle, JUnit 5 tests, JaCoCo 80% threshold, frontend lint + typecheck + Vitest + build |
| `pr-build.yml` — **PR Title Check** | PR open / edit | Conventional Commit format on PR title |
| `images.yml` — **Docker Build** | Push to `develop` | Multi-stage Temurin JRE 25 image build and GHCR publish |

### Local Pre-Push Verification (Mandatory)

Run the full local check suite **before every push**. Never rely on CI to discover basic errors.

**Backend:**

```bash
./gradlew checkstyleMain checkstyleTest test jacocoTestReport
```

**Frontend:**

```bash
cd frontend
npm run lint && npm run typecheck && npm run test && npm run build
```

Resolve all findings in a single local pass, then push once.

### JaCoCo Coverage

CI enforces a strict **80.00% minimum line coverage** threshold. Aim for **≥ 82%** locally to have a safety buffer against regressions. When adding or refactoring business logic, include both happy-path and error-path unit tests in the same commit.

---

## 4. Pull Request Workflow

### PR Title

Must be in **English** and follow the Conventional Commits format (e.g., `feat(dashboard): add market analytics KPI cards`).

### PR Labels

Apply at least one label when opening the PR:

- `enhancement` — new feature or improvement
- `bug` — defect fix
- `documentation` — documentation-only changes

### PR Description

Write the description in **concise, high-signal Hungarian** using exactly these four sections from the [pull request template](.github/pull_request_template.md):

1. **🎯 Miért készült?** — Context and goal (1–2 sentences)
2. **🛠️ Mit módosít / Mit javít?** — Key technical changes (bullet list)
3. **🧪 Tesztelés és Ellenőrzés** — Verification commands and results
4. **🛡️ Thematic Agent Approval Gate** — Sign-off from `fullstack-architect`, `scraper-security-engineer`, and `quality-devops-engineer` (status token + one-sentence rationale each)

Always close the description with a linking keyword: `Closes #<id>` or `Fixes #<id>`.

> [!CAUTION]
> Save the PR body to a UTF-8 (no BOM) file and use `gh pr create --body-file <file>` to prevent character corruption on Windows/PowerShell.

### Merging

**Never merge your own PR.** Merging is reserved for the repository owner. Do not run `gh pr merge`.

---

## 5. Issue Tracking

Use the repository issue templates as the single source of truth.

### Bug Reports

Template: [`.github/ISSUE_TEMPLATE/bug_report.yml`](.github/ISSUE_TEMPLATE/bug_report.yml)

- Title format: `fix: <short description>`
- Label: `bug`
- Required sections: problem description, expected vs actual behavior, reproduction steps, affected component, environment, logs

### Feature / Enhancement Requests

Template: [`.github/ISSUE_TEMPLATE/feature_request.yml`](.github/ISSUE_TEMPLATE/feature_request.yml)

- Title format: `feat: <short description>`
- Label: `enhancement`
- Required sections: user problem/need, proposed solution, affected specialist component/agent, alternatives

> [!NOTE]
> For CLI issue creation, mirror every required section from the YAML template in the markdown body.

---

## 6. Security & CodeQL (AppSec)

### SSRF Prevention (CWE-918)

CodeQL taint-tracking flags any `HttpClient` or raw socket sink that receives input tainted by request parameters, even through indirect sanitizers. Follow these rules:

- **Always route outbound portal requests through [`HttpFetcher`](src/main/java/ingatlanok/common/HttpFetcher.java).** It integrates `PortalRateLimiter`, `PortalCircuitBreaker`, and domain whitelisting.
- If a direct HTTP call is unavoidable, use a compile-time literal prefix guard immediately before the sink:

  ```java
  if (!url.startsWith("https://exact-portal.hu/")) {
      throw new IllegalArgumentException("Blocked: non-whitelisted URL");
  }
  ```

### Input Sanitization

Search parameters (city, price range, floor area) must be processed through the validated utility chain before reaching any network or persistence sink:

- [`SlugUtils`](src/main/java/ingatlanok/common/SlugUtils.java) — diacritic removal, slug normalization
- [`CityUtils`](src/main/java/ingatlanok/common/CityUtils.java) — city name normalization and preset resolution
- [`PropertyParser`](src/main/java/ingatlanok/common/PropertyParser.java) — price/area regex parsing

### Vulnerability Reporting

Do **not** open a public issue for security vulnerabilities. Follow the procedure in [`SECURITY.md`](SECURITY.md).

---

## 7. Docker Releases

| Workflow | Trigger | Description |
| :--- | :--- | :--- |
| `images.yml` — Docker Build | Push to `develop` | Publishes `latest` backend and frontend images to GHCR automatically |
| `release.yml` — Docker Release | Manual from `main` | Publishes versioned (`1.x.y`) images, creates Git tag and GitHub Release |

Release versions derive from the latest Git tag plus a `patch` / `minor` / `major` / custom input. Images are tagged with clean numeric SemVer (e.g., `1.2.0`) — no `v` prefix.
