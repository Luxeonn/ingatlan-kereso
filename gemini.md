# Ingatlan Kereső - System Architecture & AI Workflow Directives (gemini.md)

This document establishes the technical system architecture, 4-layer component boundaries, persistence design, web scraping specifications, and mandatory AI development and version control guidelines for **ingatlan-kereso**.

---

## 1. System Architecture & High-Level Flow

**ingatlan-kereso** is built on Java 25 LTS with Spring Boot 4.1.2, Virtual Threads, Spring Data JPA persistence (embedded H2 for dev/CLI, PostgreSQL 17 for production), a pure headless JSON REST API, and an autonomous modern React 19 SPA frontend served via Vite (dev) or Nginx reverse proxy (Docker Compose). Authoritative architecture reference: [docs/architecture.md](docs/architecture.md).

```mermaid
flowchart TD
    subgraph UI ["Frontend Layer (Browser SPA)"]
        SPA["React 19 + TypeScript + Tailwind SPA"]
        Panels["SearchPanel | ListingsPanel | MarketAnalyticsPanel | Modals"]
        SPA --> Panels
    end

    subgraph Server ["Server Layer (Headless Spring Boot 4.1.2 / Java 25)"]
        HTTP["RestApiController & DedupController (Port 8080)"]
        Pres["DashboardPresentationService & DedupPresentationService"]
        VThread["Virtual Threads Dispatcher"]
        Cors["WebCorsConfig & SecurityHeadersFilter"]
        HTTP --> VThread & Cors & Pres
    end

    subgraph Service ["Core & Scraper Layer"]
        Orchestrator["SearchExecutionEngine"]
        Normalizer["ListingNormalizer (Canonical PropertyType)"]
        Portals["Portal Engines (AbstractAdapterPortalEngine): DH | OC | Jofogas | Koltozzbe | IngatlanBazar | IngatlanCom (+ Sidecar)"]
        Import["IngatlanComHtmlImportService & PersistedFallbackMerger"]
        Quality["DataQualityEngine (0-100 Score & Outlier Filter)"]
        Analytics["MarketAnalyticsEngine & MarketStatisticsCalculator"]
        Dedup["DeduplicationEngine (+ DedupOverrides)"]

        Orchestrator --> Portals --> Normalizer --> Dedup --> Quality --> Analytics
        Orchestrator --> Import
    end

    subgraph Persistence ["Persistence Layer (3-Tier Spring Data JPA)"]
        Storage["JpaIngatlanStorage & JpaDedupOverrideStorage (ListingStorage / ConfigStorage / DedupOverrideStorage)"]
        Repo["PropertyRepository / ListingObservationRepository / PriceHistoryRepository / StatusLogRepository / ConfigRepository / SearchPresetRepository / DedupOverrideRepository"]
        H2[("data/ingatlanok_jpa.mv.db (Embedded H2 / PostgreSQL 17)")]
        Storage --> Repo --> H2
    end

    Panels -->|"/api/search, /api/dedup/*, /api/presets*"| HTTP
    VThread --> Pres --> Orchestrator
    Dedup <-->|"Cross-Portal Clustering, Overrides & Observations"| Storage
    Analytics --> Orchestrator -->|DashboardResponseDto JSON| HTTP -->|JSON Response| SPA
```

---

## 2. Package Structure & Layer Responsibilities

Each Java package includes a `package-info.java` module map documenting public entry points and ArchUnit layer boundaries.

```text
ingatlan-kereso/
├── .github/
│   ├── pull_request_template.md                 # PR checklist
│   └── workflows/                               # ci.yml, dependency-submission.yml, images.yml, pr-build.yml, pr-compliance.yml, pr-guards.yml, release.yml
├── config/checkstyle/checkstyle.xml             # Checkstyle 10.21.4 ruleset (FileLength <= 450, 2-space Indentation)
├── scripts/
│   ├── ai-verify.sh                             # Compact, token-efficient verification runner (--backend, --frontend, --all)
│   ├── start-docker.bat                         # Windows launcher (local build, GHCR latest, or custom tag)
│   ├── smoke-test-docker-compose.sh             # Manual Compose integration smoke test
│   ├── verify-pr-compliance.{mjs,sh}            # PR template, issue link, and DTO/OpenAPI sync validator + test-verify-pr-compliance.mjs
│   └── validate-skills.sh                       # Offline specialist-skill & architectural guardrail validator (7 checks: [1/7]–[7/7])
├── build.gradle.kts                             # Gradle Kotlin DSL (Java 25, Spring Boot 4.1.2, -Werror, JaCoCo >=80%)
├── Dockerfile & docker-compose.yml              # Multi-stage Temurin JRE 25 (+ Node.js/Playwright Chromium) & 3-tier stack
├── docs/architecture.md                         # Authoritative system architecture reference
├── tools/                                       # sidecar-playwright.mjs (Node.js Playwright Chromium sidecar)
├── frontend/                                    # Decoupled React 19 + TypeScript + Vite SPA (ESLint max-lines <= 350)
│   ├── src/
│   │   ├── api/                                 # client.ts (openapi-fetch), cityApi.ts, dedupApi.ts, portalApi.ts, presetApi.ts, propertyApi.ts, searchApi.ts
│   │   ├── components/
│   │   │   ├── analytics/                       # MarketAnalyticsPanel.tsx, MarketAnalyticsKpiSection.tsx, charts & tables
│   │   │   ├── filters/                         # SearchPanel.tsx, SearchPresetsBar.tsx, NumericStepper.tsx (shared AreaStepper/PriceStepper)
│   │   │   ├── listings/                        # ListingsPanel.tsx, BulkActionBar.tsx, BulkFilterChips.tsx, PropertyCard.tsx, PropertyCardParts.tsx, QualityScoreShield.tsx
│   │   │   └── modals/                          # DiagnosticsModal.tsx, IngatlanComImportModal.tsx
│   │   ├── hooks/                               # useDedupActions.ts, useMarketAnalytics.ts, useSearch.ts, useOutsideDismiss.ts
│   │   ├── store/                               # useFilterStore.ts, filterUrlSync.ts, useUiStore.ts
│   │   ├── test/                                # Vitest suites & testFixtures.ts (Object Mother mock factories)
│   │   ├── types/generated-api.ts               # Single-source-of-truth OpenAPI TypeScript schemas
│   │   └── utils/                               # formatters.ts, marketAnalyticsMath.ts, marketAnalyticsAggregation.ts
│   └── eslint.config.js & vite.config.ts        # Zero-warning ESLint & Vite config
├── src/main/java/ingatlanok/
│   ├── IngatlanApplication.java & CliSearchRunner.java # Spring Boot & CLI hybrid entry points
│   ├── common/                                  # HttpFetcher (SSRF sink), PropertyParser, CityUtils, SlugUtils, resilience/
│   ├── core/                                    # Layer 2: SearchExecutionEngine, ParallelPortalScrapeExecutor, SearchPersistenceSynchronizer, PersistedFallbackMerger, IngatlanComHtmlImportService, ListingNormalizer, PropertyFilterEngine, DeduplicationEngine (+ core/dedup/*), DataQualityEngine, MarketAnalyticsEngine, MarketStatisticsCalculator
│   ├── model/                                   # Canonical PropertyType, DedupOverride, DedupOverrideType, SearchPreset, SourceListing, ListingObservation, IngatlanItem, MarketStats, MarketSegmentStats, PriceDropItem, QualityScore, QualityDeduction, PriceInfo, PropertyDetails, PortalSource, SearchCriteria, SearchResult
│   ├── persistence/                             # Layer 4: ListingStorage, ConfigStorage, DedupOverrideStorage, JpaIngatlanStorage, JpaDedupOverrideStorage, service/{PropertyPersistenceHelper,SearchPresetPersistenceHelper}, mapper/{JpaEntityMapper,SearchPresetMapper}, entity/*, repository/*
│   ├── portal/                                  # Layer 3: PortalEngine, AbstractAdapterPortalEngine, AbstractPortalAdapter, AbstractPaginatedExtractionStrategy, AbstractPaginatedHtmlStrategy, PortalEngineRegistry, sidecar/{SidecarCliExecutor,SidecarResponse,SidecarStatus}, dh/, oc/, jofogas/, koltozzbe/, ingatlanbazar/, ingatlancom/
│   ├── presentation/                            # Layer 1a: DashboardPresentationService, DedupPresentationService, PresentationMapper, DashboardResponseDto, Dedup*Dto, IngatlanComHtmlImport*Dto, MarketStatsDto, MarketSegmentStatsDto, PriceDropItemDto, PortalStatusDto
│   ├── config/                                  # Layer 1b: WebCorsConfig, SecurityHeadersFilter, RequestLoggingFilter
│   └── web/                                     # Layer 1b: RestApiController, DedupController, GlobalExceptionHandler, HistoryQueryDto, typed response records
└── src/test/
    ├── java/ingatlanok/testsupport/TestFixtures.java # Shared Object Mother builder factory for deterministic tests
    └── resources/logback-test.xml               # Quiet WARN-level test logging to prevent console token bloat
```

---

## 3. Core Architectural Principles

1. **Spring Boot 4.1.2 & Java 25 Virtual Threads:** Headless REST service with `spring.threads.virtual.enabled=true`. Scraping I/O and REST requests never block carrier threads.
2. **0-Crash Resilience:** Network failures, DOM changes, or WAF challenges on one portal never crash the server; portal engines return structured status cards (`BOT_PROTECTED`, `FAILED`, `SPA_FALLBACK`).
3. **Decoupled 3-Tier Persistence (Dual-Mode):** Hidden behind `ListingStorage`, `ConfigStorage`, and `DedupOverrideStorage`. `JpaIngatlanStorage` (`PropertyPersistenceHelper`, `SearchPresetPersistenceHelper`) and `JpaDedupOverrideStorage` use Spring Data JPA with embedded H2 (`data/ingatlanok_jpa.mv.db`) for dev/CLI, PostgreSQL 17 for Docker Compose, and in-memory H2 for hermetic tests.
4. **Modern Decoupled Frontend & Clean Versioning:** Autonomous React 19 + TypeScript + Vite SPA served by Nginx in production. GHCR images use clean numeric SemVer tags (`1.0.0`, `1.0.0-<sha>`) without a `v` prefix.
5. **Canonical Normalization, Multi-Signal Deduplication & 3-Tier Analytics:** Portal categories map to `PropertyType`. Identical physical properties across portals cluster into a single card showing the lowest active price and a multi-source drawer (with persistent `FORCE_SPLIT` / `FORCE_MERGE` `DedupOverrides` via `DedupController`). `MarketAnalyticsEngine` and `MarketStatisticsCalculator` compute domain `MarketStats` (living vs. land area medians, bargains $\ge 20\%$ below median), while `DataQualityEngine` (0–100 score) quarantines outliers ($< 70$).
6. **Mandatory Boilerplate Elimination & Single-Source-of-Truth Patterns:**
   - **Backend (Lombok & MapStruct):** Use `@Getter`, `@Builder` (`@Builder.Default`, `toBuilder = true`), and `@NoArgsConstructor` (`@Setter` only where post-construction mutation is needed; read-only DTOs/value objects omit `@Setter`). `@Data`, `@EqualsAndHashCode`, `@ToString`, and `@SneakyThrows` are forbidden. Use MapStruct (`JpaEntityMapper`, `PresentationMapper`, `SearchPresetMapper`) with `componentModel = "spring"` and `unmappedTargetPolicy = ReportingPolicy.ERROR`.
   - **Frontend (OpenAPI Derivation & Deduplication):** Derive all frontend DTO types (`types/property.ts`, `types/api.ts`, `types/portal.ts`) from `generated-api.ts` (`components['schemas']`). Centralize helpers in `utils/formatters.ts` (`normalizePortalKey`, `PORTAL_CHIPS`, `clampQualityScore`, `getQualityConfig`, `getEffectivePrice`, `getEffectiveSqmPrice`, `openExternalUrl`) and reuse `NumericStepper`, `useOutsideDismiss`, and `SORT_OPTIONS`.
7. **Fail-Closed Compiler, Exception-Free ArchUnit & Strict Modularity Gates:**
   - **Backend Javac & ArchUnit:** Javac enforces `-Werror` and `-Xlint:unchecked,deprecation,rawtypes,fallthrough,cast,overrides,divzero,empty`. `ArchitectureRulesTest.java` enforces 4-layer unidirectional flow with **zero exceptions** (`core`, `persistence`, and `portal` never depend on `presentation`, `web`, or `config`), storage encapsulation, read-only DTO/model immutability, single-point SSRF isolation in `HttpFetcher`, and virtual thread non-pinning (no `synchronized` anywhere in `src/main/java/ingatlanok/`).
   - **Checkstyle 10.21.4:** Enforces `FileLength` <= 450, 2-space `Indentation`, `WhitespaceAround`, `WhitespaceAfter`, `NoWhitespaceBefore`, `GenericWhitespace`, `EmptyLineSeparator`, `MethodLength` <= 80, `ParameterNumber` <= 6, `CyclomaticComplexity` <= 12, `BooleanExpressionComplexity` <= 4, `NestedIfDepth` <= 3, `NestedTryDepth` <= 2, and `DeclarationOrder`.
   - **Frontend TypeScript & ESLint:** Enforces `noImplicitReturns`, `noImplicitOverride`, `forceConsistentCasingInFileNames`, ESLint `--max-warnings 0`, `max-lines: 350` (`skipBlankLines: true, skipComments: true`), `complexity: 15`, `max-depth: 4`, `max-params: 5`, `eqeqeq`, `@typescript-eslint/consistent-type-imports`, `@typescript-eslint/no-explicit-any: error`, and AST ban on direct `window.open`.
8. **AI Token-Efficiency & High-Signal Tooling:**
   - **Shared Test Fixtures (Object Mother):** Use `TestFixtures.java` (`src/test/java/ingatlanok/testsupport/TestFixtures.java`) in backend tests and `testFixtures.ts` (`frontend/src/test/testFixtures.ts`) in frontend tests instead of repeating 25-line item setup blocks.
   - **Quiet Test Logging & Compact Verification:** `src/test/resources/logback-test.xml` restricts test logs to `WARN`. Use `bash scripts/ai-verify.sh [--backend|--frontend|--all]` for compact, single-line PASS summaries or focused error excerpts.
9. **Business Logic Regression Protection & Regression Test First Protocol:**
   - Adhere strictly to the **Regression Test First** cycle documented in [docs/business-logic-regression.md](docs/business-logic-regression.md): identify and reproduce the defect, write a failing regression test (Red) proving the defect prior to implementation, apply the minimal targeted fix (Green), and verify against the versioned static Golden Test Dataset (`src/test/resources/fixtures/golden/business-regression-dataset.json`) without external network or LLM dependencies.

---

## 4. Web Scraping Specifications

| Portal | Domain | Method | Specifics & Challenge |
| :--- | :--- | :--- | :--- |
| **Duna House** | `dh.hu` | Live Jsoup DOM | Pattern: `/elado-ingatlan/<type>/<county>/<city>/<price>-mFt` |
| **Otthon Centrum** | `oc.hu` | Live Jsoup DOM | Rich card parsing with Chrome 133 `Sec-Ch-Ua` headers |
| **Jófogás Ingatlan** | `jofogas.hu` | Live Jsoup DOM | Query-based parsing with price and location regex cleaning |
| **Költözzbe** | `koltozzbe.hu` | Live DOM & JSON-LD | Dual strategy: HTML cards and Schema.org JSON-LD |
| **Ingatlanbazár** | `ingatlanbazar.hu` | Live Internal REST API | High-speed JSON endpoints (`/api/property-search`) with cached location codes |
| **Ingatlan.com** | `ingatlan.com` | Sidecar + HTML Import / Fallback | `IngatlanComSidecarStrategy` (`SidecarCliExecutor` → `tools/sidecar-playwright.mjs`, 120s budget in `ParallelPortalScrapeExecutor`), `PersistedFallbackMerger`, manual HTML import (`IngatlanComHtmlImportService`), and graceful `BOT_PROTECTED` fallback when WAF blocks |

---

## 5. AI Developer & Workflow Rules (Mandatory)

> [!IMPORTANT]
> 1. **Direct Pushes Prohibited:** Never push directly to `main` or `develop`.
> 2. **Git Worktrees & Branch Naming:** Start all branches from `develop` using the mandatory issue-based format `ik-<issue-number>` (e.g., `ik-503` for issue `#503`), using dedicated worktrees (`git worktree add -b ik-<id> ../worktrees/ik-<id> develop`) when isolating concurrent work.
> 3. **Atomic Commits:** Commit each coherent logical increment with a Conventional Commits message (`feat: ...`, `fix: ...`, `docs: ...`). Never squash unrelated work or push mechanical fixup chains.
> 4. **Pipeline Concurrency:** Cancel obsolete workflow runs (`gh run cancel <run-id>`).
> 5. **PR Conventions & UTF-8 Encoding:**
>    - **Title & Labels:** English Conventional Commit title; apply labels (`enhancement`, `bug`, `documentation`).
>    - **Hungarian 2-Section Body:** Write concise, high-signal Hungarian descriptions strictly with: **🎯 Miért készült?** and **🛠️ Mit módosít / Mit javít?**, ending with `Closes #<id>` / `Fixes #<id>`. Always pass `--body-file` encoded in clean UTF-8 without BOM.
>    - **Direct PR Link:** Always include a clickable GitHub PR link in the final response.
> 6. **Green Gate & Never Auto-Merge:** PRs are mergeable only when 100% of CI checks are green. Never run `gh pr merge`; merging is reserved for the human repository owner.
> 7. **Standardized Issue Templates:** Always follow `.github/ISSUE_TEMPLATE/bug_report.yml` (`fix: <summary>`, `bug` label) and `.github/ISSUE_TEMPLATE/feature_request.yml` (`feat: <summary>`, `enhancement` label) as the single source of truth.
> 8. **Regression Test First Discipline:** Follow the 6-step cycle in [docs/business-logic-regression.md](docs/business-logic-regression.md); prove the defect with a failing test (Red) before fixing the implementation (Green).
> 9. **Issue-Scope Boundary & Anti-Scope-Creep Guardrails:** Modifications must be strictly confined to the issue's stated goals and acceptance criteria. Unsolicited refactoring, technology version bumps, package reorganizations, or API/DB contract shifts are prohibited. Out-of-scope improvements must be documented as separate proposed issues; never execute them opportunistically. Any supporting change required to meet acceptance criteria must be explicitly justified in the Hungarian PR body.
> 10. **Adversarial Self-Review Protocol (Anti-Tunnel-Vision & Confirmation-Bias Gate):** Before opening any PR, agents must systematically audit their own implementation against four critical invariants: (1) **Multi-Source Domain Invariant** (no direct `item.priceInfo.amountHuf` / `item.detailsInfo.pricePerSquareMeter` access, use `getEffectivePrice`/`getEffectiveSqmPrice` from `frontend/src/utils/formatters.ts`), (2) **Multi-City & Partial Failure Invariant** (proper handling of mixed portal/city statuses), (3) **End-to-End Contract Traceability** (DTO -> `api-spec.yaml` -> `generated-api.ts` -> UI components), and (4) **Hermetic Test Isolation** (no production `app.test-mode` branching, zero `synchronized` carrier-pinning in Java 25).
> 11. **Cross-Portal Bug & Fix Consistency Rule:** Whenever a scraping, pagination, URL-building, or parsing bug is discovered or fixed in one portal engine, always audit all other portal engines (`dh`, `oc`, `jofogas`, `koltozzbe`, `ingatlanbazar`, `ingatlancom`) for the same defect and apply the fix uniformly across every affected portal.
> 12. **Mandatory Subagent Delegation & Issue Routing:** Actively delegate daily development tasks and GitHub issues to the 8 narrow-scope custom subagents in `.agents/agents/` (`invoke_subagent`) according to Section 7 below; always run `pr-quality-reviewer` before opening a PR.

---

## 6. Local Pre-Push Verification & CI Hygiene

Never push unverified code to remote. Run fast iterative checks via `bash scripts/ai-verify.sh --all`, and execute the full pre-push gates before pushing:

- **Backend full gate (keep JaCoCo line coverage safely $\ge 82\%$, CI minimum $80.00\%$):**
  ```bash
  ./gradlew checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification check bootJar
  ```
- **Frontend full gate:**
  ```bash
  cd frontend && npm run generate-api && npm run typecheck && npm run lint && npm run test && npm run build
  ```
- **Skills & architectural guardrails gate:**
  ```bash
  bash scripts/validate-skills.sh -v
  ```
- **AppSec & Spring CLI:** Route all external scraping HTTP calls through `HttpFetcher` (SSRF protection) and keep `@ConditionalOnProperty(name = "cli")` on `CliSearchRunner` synchronized via `System.setProperty("cli", "true")` in `IngatlanApplication.main()`.

---

## 7. Narrow-Scope Subagent Team, Issue Routing & PR Lifecycle Hooks

In daily development, prefer delegating work via `invoke_subagent` to the 8 specialized agents in `.agents/agents/` as frequently as the task warrants (in parallel where independent, or chained across handoffs). Before starting an issue (`#<id>` / `ik-<id>`), run `gh issue view <id>` and route automatically:

| Agent (`.agents/agents/<name>/agent.md`) | Write Scope | Automatic Delegation Triggers |
| :--- | :--- | :--- |
| **`product-researcher`** | Read-only | Product/UX/data growth research, Impact/Effort roadmap, GitHub Issue drafts |
| **`ui-designer`** | Read-only | Modern UI/UX specs, Tailwind v4 + CSS tokens, 360px–4K layout, WCAG 2.2 AA |
| **`qa-test-engineer`** | Tests & fixtures only | Regression Test First (**Red** failing tests first), Golden Dataset, Playwright E2E, coverage |
| **`backend-developer`** | Java web/pres/service/JPA + OpenAPI | REST controllers, MapStruct DTOs, JPA persistence, `openapi/api-spec.yaml` owner |
| **`frontend-developer`** | `frontend/src/**` | React 19, TypeScript 5.9, Tailwind v4, `npm run generate-api`, Recharts, Zustand |
| **`scraper-engineer`** | `portal/**`, `tools/**`, `HttpFetcher`, `PropertyParser` | All 6 portals, Playwright/Python sidecars, HTML import, SSRF & cross-portal fixes |
| **`domain-algorithm-engineer`** | `core/` domain engines | `DeduplicationEngine`, `core/dedup/*`, `MarketAnalyticsEngine`, `DataQualityEngine`, `ListingNormalizer` |
| **`pr-quality-reviewer`** | Read-only | Mandatory pre-PR diff, Clean Code, type-safety, ArchUnit & compliance gate (`FINDING`/`NO_FINDING`) |

**Automated Lifecycle Hooks (`.agents/hooks.json`):**
- **`pr-publish-guard` (`PreToolUse` on `run_command`):** Runs `.agents/hooks/pr-publish-guard.mjs` before `gh pr create`, `gh pr merge`, or `git push` to block auto-merge, direct pushes to `main`/`develop`, invalid branch/title formats, UTF-8 BOM or non-compliant PR bodies (`verify-pr-compliance.mjs`), dirty tracked files, and failing `scripts/validate-skills.sh`.
- **`issue-subagent-router` (`SessionStart` / `PreInvocation`):** Runs `.agents/hooks/issue-subagent-router.mjs` to map issue numbers and domain keywords to the exact subagent pipeline automatically.
