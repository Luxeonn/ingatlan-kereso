# Közreműködői útmutató

Ez az útmutató foglalja össze az **ingatlan-kereso** fejlesztési munkafolyamatát: az ágkezelést, commit-konvenciókat, kötelező helyi ellenőrzéseket, PR-szabályokat és az aktív CI/CD folyamatokat.

## 1. Branch-stratégia és Git Worktree

Közvetlen push a `main` és `develop` ágakra **tilos**. Minden fejlesztés a `develop` ágról induló saját ágon vagy izolált worktree-ben készül, kötelezően `ik-<issue-szám>` elnevezéssel (ahol az `ik` az *ingatlan-kereső* rövidítése, pl. `ik-503` a `#503`-as issue esetén):

```bash
git worktree add -b ik-<id> ../worktrees/ik-<id> develop
```

Az ág típusát és leírását a Conventional Commits formátumú PR-cím, valamint a PR-leírás végén szereplő `Closes #<id>` hivatkozás hordozza.

## 2. Commit üzenetek

Minden commit angol nyelvű **Conventional Commits** formátumot követ:

```text
<type>(<scope>): <short imperative description>
```

Példák: `feat(search): add area filter`, `fix(portal): handle missing price on Jófogás`, `refactor(dedup): extract helper`, `docs: update README`.

Logikai egységenként commitolj, és csak helyben ellenőrzött, tiszta állapotot pusholj (ne publikálj hibakereső „fixup” láncokat).

## 3. Lokális ellenőrzés (push előtt kötelező)

**Gyors összevont ellenőrzés:**

```bash
bash scripts/ai-verify.sh --all
```

**Teljes backend kapu** *(Java 25, Checkstyle, ArchUnit, JUnit 5, JaCoCo ≥ 80%, javasolt ≥ 82%)*:

```bash
./gradlew checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification check bootJar
```

**Teljes frontend kapu** *(OpenAPI típusgenerálás, TypeScript, ESLint, Vitest, Vite build, Playwright E2E)*:

```bash
cd frontend && npm run generate-api && npm run typecheck && npm run lint && npm run test && npm run build
npm run test:e2e
```

**Guardrail és PR-megfelelőség ellenőrzése:**

```bash
bash scripts/validate-skills.sh -v
node scripts/verify-pr-compliance.mjs --body-file pr_body.md --diff-base origin/develop -v
```

## 4. Pull Request és Issue folyamat

- **PR cím és címke:** Angol nyelvű Conventional Commit cím + megfelelő címke (`enhancement`, `bug`, `documentation`).
- **PR leírás:** Tömör magyar szöveg **BOM nélküli UTF-8** fájlban (`gh pr create --body-file ...`), a [`.github/pull_request_template.md`](.github/pull_request_template.md) két szekciójával (`## 🎯 Miért készült?`, `## 🛠️ Mit módosít / Mit javít?`) és záró hivatkozással (`Closes #<id>` / `Fixes #<id>`). Ha `presentation/*Dto.java` változik, az [`openapi/api-spec.yaml`](openapi/api-spec.yaml) és a `frontend/src/types/generated-api.ts` fájlokat is frissíteni kell.
- **Review és merge:** Mindig kérj `@copilot` reviewt (`gh pr edit <id> --add-reviewer "@copilot"`). Saját PR merge-elése (`gh pr merge`) tilos; az összevonást a karbantartó végzi zöld CI után.
- **Issue sablonok:** Hibákhoz [`.github/ISSUE_TEMPLATE/bug_report.yml`](.github/ISSUE_TEMPLATE/bug_report.yml) (`fix: ...`, `bug`), fejlesztésekhez [`.github/ISSUE_TEMPLATE/feature_request.yml`](.github/ISSUE_TEMPLATE/feature_request.yml) (`feat: ...`, `enhancement`).

## 5. Aktív CI/CD Workflow-k és AppSec

| Workflow | Esemény | Ellenőrzés / Feladat |
| :--- | :--- | :--- |
| `ci.yml` (**CI Build**) | Push / PR (`develop`, `main`) | Gitleaks, OpenAPI drift, frontend + Playwright E2E, backend (Checkstyle, ArchUnit, JUnit 5, JaCoCo ≥ 80%, smoke test), Trivy, `validate-skills.sh` |
| `pr-build.yml` (**PR build**) | PR (`develop`, `main`) | Conventional Commit / verziószám alapú PR cím ellenőrzése |
| `pr-compliance.yml` (**PR Compliance**) | PR (`develop`, `main`) | 2 magyar szekció, `Closes #`, OpenAPI DTO szinkron (`verify-pr-compliance.mjs`), `validate-skills.sh` |
| `pr-guards.yml` (**PR Guards**) | PR (`develop`) | Ellenőrzi, hogy a PR ág naprakész-e a `develop` ághoz képest |
| `images.yml` (**Docker Build**) | Push (`develop`) | Teljes `ci.yml` futtatás után `:latest` képek publikálása GHCR-re |
| `release.yml` (**Docker Release**) | Manuális, **kizárólag `main`** | Verziózott SemVer képek (`:x.y.z`), `v*` Git tag és GitHub Release létrehozása |
| `dependency-submission.yml` | Push / Manuális | Gradle függőségi gráf beküldése a GitHub Dependency Graph felé |

- **AppSec alapszabály:** Minden kimenő portál HTTP hívást a [`HttpFetcher`](src/main/java/ingatlanok/common/HttpFetcher.java) osztályon keresztül kell futtatni. Sérülékenységet tilos nyilvános issue-ban közölni — kövesd a [`SECURITY.md`](SECURITY.md) eljárását.
