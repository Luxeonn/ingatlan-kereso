---
name: workflow-engineer
description: >-
  CI/CD automation and GitHub Actions specialist. Use this skill to design, harden, and optimize
  GitHub Actions workflows (.github/workflows/), maintain PR guard policies, enforce quality gates,
  and automate container build and publishing processes.
metadata:
  icon: terminal
---

# Workflow Engineer & GitHub Actions Specialist Skill

This skill defines standards for robust, secure, and cost-efficient **GitHub Actions CI/CD workflows** for **ingatlan-kereso**.

---

## 1. Core Principles & Hardening Standards

### 1.1 Security & Least Privilege
* **Explicit `permissions` Block:** Declare minimal required permissions per workflow and job (e.g., `contents: read`, `packages: write`).
* **Shell Injection Prevention:** Never interpolate untrusted GitHub event contexts directly in `run:` scripts. Pass them via environment variables:
  ```yaml
  env:
    PR_TITLE: ${{ github.event.pull_request.title }}
  run: echo "PR title: $PR_TITLE"
  ```
* **Secret Protection:** Pass sensitive credentials only from `secrets.*` into environment variables.

### 1.2 Pipeline Efficiency & Concurrency
* **Concurrency Management:** Cancel redundant in-progress runs when a new commit is pushed:
  ```yaml
  concurrency:
    group: ${{ github.workflow }}-${{ github.ref }}
    cancel-in-progress: true
  ```
* **Job Timeouts:** Always declare `timeout-minutes` (typically 10-15 minutes) to prevent runaway jobs.
* **Intelligent Caching:** Utilize Maven cache (`actions/setup-java` with `cache: maven`) and Docker Buildx cache (`cache-from: type=gha`, `cache-to: type=gha,mode=max`).

### 1.3 Observability
* Every workflow must write a structured markdown status summary to `$GITHUB_STEP_SUMMARY`.

---

## 2. Key Workflow Types

### 2.1 PR & Branch Protection Guard (`pr-guard.yml`)
* **Trigger:** `pull_request` on `main` and `develop`.
* **Checks:** Conventional Commits title, branch prefix convention, linked issues, and Thematic Agent Approval Gate.
* **Gate:** Enforces that the PR is **strictly mergeable ONLY when all checks are green**.

### 2.2 Continuous Integration (`ci.yml`)
* **Trigger:** `push` and `pull_request` on `main` and `develop`.
* **Jobs:**
  1. Frontend build, TypeScript validation, and Vitest suite execution.
  2. Maven clean, test, and package (Java 21).
  3. Headless runtime smoke test (boot server, verify root div, validate live JS bundle with `node --check`, check CORS preflight).

### 2.3 Container Build & GHCR Publish (`docker-publish.yml`)
* **Trigger:** `push` to `develop`, release tags (`v*`), or `workflow_dispatch`.
* **Tagging Strategy:** `develop` -> `:latest`; tags/releases -> exact version tag (prohibits floating tag overwrite).

---

## 3. Workflow Engineer Checklist

- [ ] All variables passed via `env:` (no shell injection risk).
- [ ] Explicit `permissions` defined.
- [ ] `concurrency` with `cancel-in-progress: true` enabled.
- [ ] Explicit `timeout-minutes` defined for each job.
- [ ] Caching configured for Maven and Docker Buildx.
- [ ] Only mergeable when 100% of pipeline checks are green.
- [ ] Clear status reported to `$GITHUB_STEP_SUMMARY`.
