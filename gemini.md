# Ingatlan Kereső - System Architecture & AI Workflow Directives (GEMINI.md)

This document establishes the technical system architecture, 4-layer component boundaries, persistence design, web scraping specifications, and mandatory AI development and version control guidelines for **ingatlan-kereso**.

---

## 1. System Architecture & High-Level Flow

**ingatlan-kereso** is built on Java 21 LTS with Spring Boot 4.1.0 (Spring Framework 7.0.9), GraalVM Native Image support (AOT), Virtual Threads, Spring Data JPA persistence (embedded H2 for dev/CLI, PostgreSQL 17 for production), a pure headless JSON REST API, and an autonomous modern React 19 SPA frontend served independently via Vite (dev) or Nginx reverse proxy (Docker Compose). Authoritative architecture reference: [docs/architecture.md](docs/architecture.md).

### Workflow Architecture

```mermaid
flowchart TD
    subgraph UI ["Frontend Layer (Browser SPA)"]
        SPA["React 19 + TypeScript + Tailwind SPA"]
        Event["Search / Filter Modification"]
        SPA --> Event
    end

    subgraph Server ["Server Layer (Headless Spring Boot 4.1.0 / Java 21)"]
        HTTP["Spring MVC REST Controllers (Port 8080)"]
        VThread["Virtual Threads Dispatcher"]
        Cors["GlobalCorsFilter & SpaWebMvcConfig (CORS & MVC Config)"]
        HTTP --> VThread
        HTTP --> Cors
    end

    subgraph Service ["Core & Scraper Layer"]
        Orchestrator["IngatlanSearchService / SearchExecutionEngine"]
        P1["DunaHousePortalEngine"]
        P2["OtthonCentrumPortalEngine"]
        P3["JofogasPortalEngine"]
        P4["KoltozzbePortalEngine"]
        P5["IngatlanBazarPortalEngine"]
        P6["IngatlanComPortalEngine (Fallback)"]
        
        Orchestrator --> P1 & P2 & P3 & P4 & P5 & P6
    end

    subgraph Persistence ["Persistence Layer (Spring Data JPA & Storage Abstraction)"]
        Storage["JpaIngatlanStorage (ListingStorage / ConfigStorage)"]
        Repo["PropertyRepository / ConfigRepository / HistoryRepository"]
        H2[("data/ingatlanok_jpa.mv.db (Embedded H2 / JPA)")]
        
        Storage --> Repo --> H2
    end

    Event -->|POST /api/search| HTTP
    VThread --> Orchestrator
    P1 & P2 & P3 & P4 & P5 & P6 -->|Parsed Listings| Storage
    Storage -->|Deduplication & New Flag| Orchestrator
    Orchestrator -->|SearchResult JSON| HTTP
    HTTP -->|JSON Response| SPA
```

---

## 2. Package Structure & Layer Responsibilities

```text
ingatlan-kereso/
├── .github/
│   ├── pull_request_template.md                 # PR checklist & Thematic Agent Gate template
│   └── workflows/
│       ├── ci.yml                               # Java 21, Checkstyle, JaCoCo, Playwright E2E
│       ├── docker-publish.yml                   # Automated GHCR container publishing (1.0.0 tags)
│       └── pr-review.yml                        # PR Guard (Conventional Commits, Branch, Thematic Gate)
├── config/
│   └── checkstyle/checkstyle.xml                # Checkstyle 10.21.4 ruleset
├── scripts/
│   ├── start-docker.bat                         # Windows launcher (local build or GHCR latest)
│   ├── smoke-test-docker-compose.sh             # Manual Compose integration smoke test
│   └── validate-skills.sh                       # Offline specialist-skill validation used by CI
├── build.gradle.kts                             # Gradle Kotlin DSL (Java 21, Spring Boot 4.1.0, 1.0.0)
├── settings.gradle.kts                          # Gradle root project settings
├── gradlew / gradlew.bat                        # Gradle Wrapper binaries
├── Dockerfile                                   # Multi-stage Temurin JRE 21 runtime container
├── docker-compose.yml                           # 3-tier production stack (Postgres, Backend, Nginx UI)
├── README.md                                    # Comprehensive user and developer guide
├── gemini.md                                    # Technical architecture & AI workflow directives
├── data/
│   └── ingatlanok_jpa.mv.db                     # Embedded H2 / JPA database
├── frontend/                                    # Modern Decoupled React 19 SPA client
│   ├── src/                                     # React 19, TypeScript, Zustand, TanStack Query
│   ├── package.json                             # Frontend dependencies, Vitest, Playwright scripts
│   ├── nginx.conf                               # Production SPA reverse proxy configuration
│   ├── Dockerfile                               # Production Nginx frontend container
│   └── vite.config.ts                           # Vite configuration with API proxy and Vitest
└── src/main/java/ingatlanok/
    ├── IngatlanApplication.java                 # Spring Boot application entry point (@SpringBootApplication)
    ├── Main.java                                # Hybrid entry point (Spring Boot delegate & CLI mode)
    ├── common/                                  # Shared cross-cutting utilities
    │   ├── BrowserUtils.java                    # Desktop browser launcher with limits
    │   ├── CityUtils.java                       # Hungarian city normalization and presets
    │   ├── HttpFetcher.java                     # Jsoup HTTP client (User-Agent, headers, timeouts)
    │   ├── PropertyParser.java                  # Price, floor area, and room regex parsing
    │   └── SlugUtils.java                       # Diacritic removal and search slug builder
    ├── core/                                    # Layer 2: Pure business logic engines
    │   ├── PropertyFilterEngine.java            # Search criteria filtering (price, area, type)
    │   ├── DeduplicationEngine.java             # O(1) deduplication and new listing detection
    │   ├── MarketAnalyticsEngine.java           # Market metrics (average price, per m² price)
    │   └── SearchExecutionEngine.java           # Parallel scraping coordinator (Virtual Threads, 15s timeout)
    ├── model/                                   # Domain models and value objects
    │   ├── IngatlanItem.java                    # Rich listing data model
    │   ├── PortalSource.java                    # Portal metadata (id, badge color, direct link)
    │   ├── PriceInfo.java                       # Structured price (raw, numeric HUF & Million HUF)
    │   ├── PropertyDetails.java                 # Floor area m², room count, calculated price/m²
    │   ├── SearchCriteria.java                  # Multi-city search criteria
    │   └── SearchResult.java                    # Aggregated search results and metrics
    ├── persistence/                             # Layer 4: Storage Abstraction & Spring Data JPA
    │   ├── ListingStorage.java                  # Storage interface for listings
    │   ├── ConfigStorage.java                   # Storage interface for configuration
    │   ├── entity/                              # JPA Entities (PropertyEntity, PriceHistoryEntity, etc.)
    │   ├── repository/                          # Spring Data JPA Repositories
    │   ├── JpaIngatlanStorage.java              # Primary Spring Data JPA implementation
    │   ├── InMemoryListingStorage.java          # In-memory storage fallback implementation
    │   └── IngatlanRepository.java              # Thread-safe Singleton & IoC bridge facade
    ├── portal/                                  # Layer 3: Pluggable portal engines (OCP)
    │   ├── PortalEngine.java                    # Uniform portal engine interface
    │   ├── PortalDescriptor.java                # Portal metadata record
    │   ├── PortalEngineRegistry.java            # Dynamic portal provider registry
    │   ├── dh/                                  # Duna House engine (DOM parsing)
    │   ├── oc/                                  # Otthon Centrum engine (DOM + client hints)
    │   ├── jofogas/                             # Jófogás engine (DOM parsing)
    │   ├── koltozzbe/                           # Költözzbe engine (DOM & Schema.org JSON-LD)
    │   ├── ingatlanbazar/                       # Ingatlanbazár engine (Internal REST API)
    │   └── ingatlancom/                         # Ingatlan.com fallback engine (Cloudflare WAF)
    ├── presentation/                            # Layer 1a: Presentation and DTO models
    │   ├── DashboardPresentationService.java    # Presentation facade (KPIs, portal health)
    │   ├── DashboardResponseDto.java            # Enriched client response payload
    │   ├── MarketStatsDto.java                  # Market analytics KPIs
    │   └── PortalStatusDto.java                 # Portal health status cards
    ├── scraper/                                 # Portal scraping data models and resolvers
    ├── config/                                  # Layer 1b: Application & Web MVC Configuration
    │   ├── CoreEngineConfig.java                # Core bean definitions
    │   ├── GlobalCorsFilter.java                # Global CORS & Preflight Filter (@Order HIGHEST)
    │   ├── GraalVmRuntimeHints.java             # GraalVM AOT reflection hints
    │   └── SpaWebMvcConfig.java                 # Spring WebMvcConfigurer (CORS & headless MVC configuration)
    └── web/                                     # Layer 1b: Embedded Web Server & REST API
        ├── RestApiController.java              # Spring MVC REST Controller (@RestController)
        └── GlobalExceptionHandler.java          # Structured JSON REST Exception Handler (@RestControllerAdvice)
```

---

## 3. Core Architectural Principles

1. **Spring Boot 4.1.0 & GraalVM Native Image:** High-performance, cloud-native execution with Ahead-of-Time (AOT) compilation and sub-second startup.
2. **Java 21 Virtual Threads:** High concurrency with lightweight virtual threads (`spring.threads.virtual.enabled=true`). Scraping I/O and REST requests never block carrier threads.
3. **0-Crash Resilience:** Network failures, DOM alterations, or bot protection triggers on one portal never crash the server. Portal engines handle exceptions gracefully and return structured status cards (`BOT_PROTECTED`, `ERROR`).
4. **Decoupled Persistence & Spring Data JPA (Dual-Mode):** Storage logic is hidden behind `ListingStorage` and `ConfigStorage`. The primary implementation is `JpaIngatlanStorage` utilizing Spring Data JPA with embedded H2 (`data/ingatlanok_jpa.mv.db`) for dev/CLI, PostgreSQL 17 for production Docker Compose, and in-memory H2 for hermetic tests, maintaining O(1) deduplication and fast queries without relational lock-in.
5. **Modern Decoupled Frontend:** Autonomous React 19 + TypeScript + Vite SPA client with online Google Fonts CDN typography, served by Nginx in containerized environments with reverse proxy routing to the headless Spring Boot REST API.
6. **Containerization & Clean Numeric Versioning:** Multi-stage Temurin JRE 21 Docker image and Docker Compose 3-tier architecture. Container images published to GHCR strictly follow clean numeric SemVer tags (`1.0.0`, `1.0.0-<sha>`) without redundant `v` prefix.

---

## 4. Web Scraping Specifications

| Portal | Domain | Method | Specifics & Challenge |
| :--- | :--- | :--- | :--- |
| **Duna House** | `dh.hu` | Live Jsoup DOM | Pattern: `/elado-ingatlan/<type>/<county>/<city>/<price>-mFt` |
| **Otthon Centrum** | `oc.hu` | Live Jsoup DOM | Rich card parsing with Chrome 133 `Sec-Ch-Ua` headers |
| **Jófogás Ingatlan** | `jofogas.hu` | Live Jsoup DOM | Query-based parsing with price and location regex cleaning |
| **Költözzbe** | `koltozzbe.hu` | Live DOM & JSON-LD | Dual strategy: HTML cards and Schema.org JSON-LD |
| **Ingatlanbazár** | `ingatlanbazar.hu` | Live Internal REST API | High-speed JSON endpoints (`/api/property-search`) with cached location codes |
| **Ingatlan.com** | `ingatlan.com` | Graceful Fallback | Cloudflare WAF protected. Generates direct search link fallback without blocking |

---

## 5. AI Developer & Workflow Rules (Mandatory)

> [!IMPORTANT]
> **Strict Git, PR, and Pipeline Rules for AI Assistants:**
>
> 1. **Direct Pushes Prohibited:** Pushing directly to `main` or `develop` is strictly forbidden.
> 2. **Git Worktrees for Issue Work:** When working on an issue, **always work inside a dedicated `git worktree`** to ensure clean isolation of working directories and branches:
>    ```bash
>    git worktree add -b <branch-name> ../worktrees/<branch-name> develop
>    ```
> 3. **Branch Naming Convention:** All branches must start from `develop` and follow the prefix format:
>    `feature/<short-desc>`, `fix/<short-desc>`, `docs/<short-desc>`, `refactor/<short-desc>`, `chore/<short-desc>`.
> 4. **Atomic Commits – No Squash During Development:**
>    - NEVER combine unrelated changes or squash work in progress.
>    - Commit each logical increment as a separate, discrete commit with a Conventional Commits message (`feat: ...`, `fix: ...`).
> 5. **Pipeline Concurrency & Cancellation:**
>    - Only the current, active pipeline run should execute.
>    - Obsolete runs must be automatically canceled (`concurrency: cancel-in-progress: true`) or manually terminated via GitHub CLI (`gh run cancel <run-id>`).
> 6. **English PR Titles:** The Pull Request title must be in English following Conventional Commits (e.g., `feat: implement market analytics dashboard`).
> 7. **Mandatory PR Labels:** Apply appropriate labels (`enhancement`, `bug`, `documentation`) upon PR creation.
> 8. **Descriptive Summary:** PR description must clearly explain Context & Goal, Technical Changes, and Verification steps.
> 9. **Direct PR Link:** Always output a clickable GitHub PR link in the final conversation response.
> 10. **Thematic Agent Approval Gate:**
>     - Every PR requires sign-off from designated specialist agents (`fullstack-architect`, `scraper-security-engineer`, `quality-devops-engineer`).
>     - Each agent must provide the standard status token (`[NO_FINDING]`, `[FINDING]`, `[NOT_APPLICABLE]`, or `[NOT_VERIFIED]`; shorthand `[OK]`, `[NOK]`, `[N/A]` also supported) and exactly **one concise sentence explaining** the rationale. Refer to [docs/architecture.md](docs/architecture.md) for domain specifications.
> 11. **ONLY MERGE WHEN ALL CHECKS ARE GREEN:**
>     - Pull requests are strictly mergeable **ONLY IF 100% OF CI AND PR GUARD CHECKS PASS (GREEN)**.
>     - If any check fails, immediately execute the **Fix Loop** (diagnose, commit fix, push, re-verify).
> 12. **Never Auto-Merge:** The AI assistant must never run `gh pr merge` or merge branches. Merging is strictly reserved for the human repository owner.
> 13. **Auto-Closing Linked Issues:** Always include official GitHub issue keywords (`Closes #<id>`, `Fixes #<id>`) in the PR description.
> 14. **Clean Numeric Versioning & Docker Image Tagging:**
>     - Release versioning strictly follows Semantic Versioning (`1.0.0`).
>     - Automated GHCR container publishing on `main` emits clean numeric tags (`1.0.0`, `1.0.0-<sha>`) without redundant `v` prefix.
>     - Manual `workflow_dispatch` releases on any branch dynamically generate `<version>-<branch>` (or `<version>-<custom_suffix>`) tags.

