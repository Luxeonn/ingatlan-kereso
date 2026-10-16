### 🛡️ Thematic Agent Approval Gate

> [!IMPORTANT]
> Section 5 of [GEMINI.md](file:///e:/github/ingatlan-kereso/gemini.md) and `.github/workflows/pr-guard.yml` mandate designated specialist approvals. Each reviewer must provide **exactly one concise sentence explaining** why the domain is marked **`[OK]`**, **`[NOK]`**, or **`[N/A]`**.

- [ ] Fullstack Architecture & UI (`fullstack-architect`): [OK / NOK / N/A] – [1-sentence rationale]
- [ ] Scraper & Security (`scraper-security-engineer`): [OK / NOK / N/A] – [1-sentence rationale]
- [ ] Quality & DevOps (`quality-devops-engineer`): [OK / NOK / N/A] – [1-sentence rationale]

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
- [ ] Gradle test execution: ./gradlew check (clean pass, 0 errors)
- [ ] Frontend test execution: cd frontend && npm test
- [ ] Smoke test steps -->

---

## Pre-Submission Checklist

- [ ] PR title strictly follows Conventional Commits (`feat: ...`, `fix: ...`, `docs: ...`).
- [ ] Source branch follows naming convention (`feature/...`, `fix/...`, `docs/...`).
- [ ] Source branch originates from `develop` and targets `develop`.
- [ ] Discrete atomic commits used without squashing work-in-progress.
- [ ] Appropriate GitHub label added (`enhancement`, `bug`, `documentation`).
- [ ] 🟢 **ONLY MERGE WHEN ALL CHECKS ARE GREEN:** 100% of CI and PR Guard checks must pass.
