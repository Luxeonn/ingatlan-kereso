### 🛡️ Thematic Agent Approval Gate

> [!IMPORTANT]
> Section 5 of [GEMINI.md](../gemini.md) and `.github/workflows/pr-review.yml` mandate designated specialist approvals.
> Each reviewer must check the box (`[x]`), provide a valid decision token (`[NO_FINDING]` / `[OK]`, `[NOT_APPLICABLE]` / `[N/A]`, or `[FINDING]` / `[NOK]`, `[NOT_VERIFIED]`), and write **exactly one concise sentence explaining** the technical rationale.
> A PR can only be merged if 100% of touched domains are signed off with a positive token (`[NO_FINDING]` / `[OK]` or `[NOT_APPLICABLE]` / `[N/A]`).

- [ ] Fullstack Architecture & UI (`fullstack-architect`): [NO_FINDING / NOT_APPLICABLE / FINDING / NOT_VERIFIED] – [1-sentence rationale]
- [ ] Scraper & Security (`scraper-security-engineer`): [NO_FINDING / NOT_APPLICABLE / FINDING / NOT_VERIFIED] – [1-sentence rationale]
- [ ] Quality & DevOps (`quality-devops-engineer`): [NO_FINDING / NOT_APPLICABLE / FINDING / NOT_VERIFIED] – [1-sentence rationale]

---

## Context & Goal

<!-- Provide a concise summary of why this change was made and what problem it solves. -->

---

## Detailed Technical Changes

<!-- Detail changes at the component and file level:
1. Which classes, modules, and interfaces were modified and why?
2. What design patterns or algorithms were introduced? -->

---

## Related Issues

<!-- Use official auto-closing keywords (e.g., Closes #123, Fixes #456). -->
- Closes #

---

## User & Operational Impact

<!-- What does the end user or operations team observe after this change? -->

---

## How to Validate

<!-- Detail automated and manual verification steps:
- [ ] Skills & instructions validation: bash scripts/validate-skills.sh
- [ ] Gradle test & verification: ./gradlew check (clean pass, 0 errors)
- [ ] Frontend test execution: cd frontend && npm test
- [ ] Docker Compose smoke test: bash scripts/smoke-test-docker-compose.sh -->

---

## Pre-Submission Checklist

- [ ] PR title strictly follows Conventional Commits (`feat: ...`, `fix: ...`, `docs: ...`).
- [ ] Source branch follows naming convention (`feature/...`, `fix/...`, `docs/...`).
- [ ] Source branch originates from `develop` and targets `develop`.
- [ ] Discrete atomic commits used without squashing work-in-progress.
- [ ] Appropriate GitHub label added (`enhancement`, `bug`, `documentation`, `architecture`, `ci`).
- [ ] 🟢 **ONLY MERGE WHEN ALL CHECKS ARE GREEN:** 100% of CI and PR Review (Guard) checks must pass.
