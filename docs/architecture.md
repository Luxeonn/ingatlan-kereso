# Ingatlan Kereső — Central System Architecture Reference

**Version**: 1.0.0  
**Status**: Authoritative Reference Specification  
**Tech Stack Baseline**: Java 25 LTS, Spring Boot 4.1.1, Spring Data JPA, Hibernate, Embedded H2 / PostgreSQL 17, React 19, TypeScript 5.9, Tailwind CSS v4, Vite 8.3, Nginx, Docker Compose.

---

## 1. System Vision & High-Level Architecture

**ingatlan-kereso** is a resilient real estate monitoring and market intelligence engine for the Hungarian property market. It continuously or on-demand aggregates listings across major national portals, filters them against complex multi-city criteria, deduplicates listings in $O(1)$ time, tracks historical price movements, computes per-square-meter market metrics, and provides an interactive user experience via an autonomous React 19 Single Page Application (SPA).

### 1.1 Architectural Flow

```mermaid
flowchart TD
    subgraph UI ["Frontend Client (React 19 SPA)"]
        SPA["React 19 + TypeScript + Tailwind SPA\n(Zustand UI State, TanStack Query Server Cache)"]
        Event["Search / Filter Modification / Property Inspection"]
        SPA --> Event
    end

    subgraph Proxy ["Edge & Ingress"]
        DevProxy["Vite Dev Proxy :5173\n(Development Mode)"]
        ProdProxy["Nginx Reverse Proxy :80/:8080\n(Production Docker Stack)"]
    end

    subgraph Server ["Server Layer (Headless Spring Boot 4.1.1 / Java 25)"]
        REST["RestApiController (/api/*)"]
        Cors["GlobalCorsFilter & SpaWebMvcConfig (CORS & Preflight)"]
        ExHandler["GlobalExceptionHandler (@RestControllerAdvice)"]
        VThread["Java 25 Virtual Threads Dispatcher\n(Executors.newVirtualThreadPerTaskExecutor)"]
        
        REST --> Cors
        REST --> ExHandler
        REST --> VThread
    end

    subgraph Core ["Core Business Logic & Orchestration"]
        Orchestrator["SearchExecutionEngine / IngatlanSearchService"]
        Filter["PropertyFilterEngine"]
        Dedup["DeduplicationEngine"]
        Analytics["MarketAnalyticsEngine"]
        ParallelScraper["ParallelPortalScrapeExecutor"]
        Aggregator["SearchResultAggregator"]
        
        Orchestrator --> ParallelScraper
        ParallelScraper --> Filter
        Filter --> Dedup
        Dedup --> Analytics
        Analytics --> Aggregator
    end

    subgraph Portal ["Portal Scraping Layer (OCP Pluggable Engines)"]
        Registry["PortalEngineRegistry"]
        P1["DunaHousePortalEngine (dh.hu)"]
        P2["OtthonCentrumPortalEngine (oc.hu)"]
        P3["JofogasPortalEngine (jofogas.hu)"]
        P4["KoltozzbePortalEngine (koltozzbe.hu)"]
        P5["IngatlanBazarPortalEngine (ingatlanbazar.hu)"]
        P6["IngatlanComPortalEngine (ingatlan.com fallback)"]
        
        Registry --> P1 & P2 & P3 & P4 & P5 & P6
    end

    subgraph Persistence ["Decoupled Persistence Layer (Spring Data JPA)"]
        Storage["JpaIngatlanStorage\n(ListingStorage & ConfigStorage Contracts)"]
        Repos["PropertyRepository / ConfigRepository\nPriceHistoryRepository / StatusLogRepository"]
        H2[("Development / CLI:\nEmbedded H2 (data/ingatlanok_jpa.mv.db)")]
        PG[("Production Stack:\nPostgreSQL 17 Container")]
        MemH2[("Hermetic Tests:\nIn-Memory H2 (jdbc:h2:mem:testdb)")]
        
        Storage --> Repos
        Repos -.-> H2
        Repos -.-> PG
        Repos -.-> MemH2
    end

    Event -->|Dev Mode| DevProxy
    Event -->|Production| ProdProxy
    DevProxy -->|Proxy /api| REST
    ProdProxy -->|Proxy /api| REST
    VThread --> Orchestrator
    ParallelScraper --> Registry
    Dedup <-->|O(1) URL Deduplication & New Flag| Storage
    Aggregator -->|DashboardResponseDto| REST
    REST -->|Pure JSON Payload| SPA
```

---

## 2. 4-Layer Separation & Component Boundaries

The codebase strictly enforces unidirectional dependencies across four decoupled architectural layers. Reverse dependency imports (layer inversion) are prohibited:

```text
[Layer 1: Presentation & Web]
    │  - presentation: DashboardPresentationService, DTOs (ListingDto, DashboardResponseDto, MarketStatsDto, PortalStatusDto)
    │  - web: RestApiController, GlobalExceptionHandler
    │  - config: GlobalCorsFilter, SpaWebMvcConfig, CoreEngineConfig
    ▼
[Layer 2: Core Domain & Business Logic]
    │  - core: SearchExecutionEngine, ParallelPortalScrapeExecutor, SearchResultAggregator,
    │          PropertyFilterEngine, DeduplicationEngine, MarketAnalyticsEngine
    │  - model: IngatlanItem, PriceInfo, PropertyDetails, PortalSource, SearchCriteria, SearchResult
    │  - service: IngatlanSearchService
    ▼
[Layer 3: Pluggable Portal Engines (Open/Closed Principle)]
    │  - portal: PortalEngine, PortalDescriptor, PortalEngineRegistry
    │  - portal.{dh, oc, jofogas, koltozzbe, ingatlanbazar, ingatlancom}
    │  - scraper: PortalScrapeResult, PortalScrapeStatus, SidecarCliExecutor
    ▼
[Layer 4: Decoupled Persistence]
       - persistence: ListingStorage, ConfigStorage, JpaIngatlanStorage, InMemoryListingStorage, IngatlanRepository
       - persistence.entity: PropertyEntity, PriceHistoryEntity, StatusLogEntity, ConfigEntity
       - persistence.repository: PropertyRepository, ConfigRepository, PriceHistoryRepository, StatusLogRepository
       - persistence.mapper: JpaEntityMapper
       - persistence.service: ListingFilterService, PriceHistoryTracker, StatusLogTracker
```

### Layer Boundary Invariants:
1. **Layer 1 never enters Layer 2, 3, or 4**: Neither `core`, `portal`, nor `persistence` may import `@RestController`, `HttpServletRequest`, or web-specific types.
2. **Core delegates to Storage Interfaces**: Layer 2 interacts with persistence exclusively through `ListingStorage` and `ConfigStorage` interfaces, never raw SQL, Hibernate sessions, or Spring Data repositories directly.
3. **Open/Closed Portal Engines**: New property portals are introduced by implementing `PortalEngine` and registering in `PortalEngineRegistry` without modifying `SearchExecutionEngine`.
4. **Leaf Utilities (`common`)**: Pure helpers (`HttpFetcher`, `PropertyParser`, `SlugUtils`, `CityUtils`, `AppLogger`) have zero dependencies on business logic or storage.

---

## 3. Backend Architecture (Java 25 LTS & Spring Boot 4.1.1)

### 3.1 Pure Headless REST API
Spring Boot operates in 100% headless mode. Monolithic static asset serving is disabled in `application.yml`:
```yaml
spring:
  web:
    resources:
      add-mappings: false
```
The backend exclusively processes and emits JSON. The browser client is served independently.

### 3.2 REST API Endpoint Catalog

| Endpoint | HTTP Method | Request Body / Query Params | Response Type | Description |
| :--- | :---: | :--- | :--- | :--- |
| `/api/status` | `GET` | None | `Map<String, Object>` | Health check, total listings count, last run timestamp, tracked cities. |
| `/api/portals` | `GET` | None | `List<PortalDescriptor>` | Active portal metadata, engine status, and saved listing counts. |
| `/api/cities` | `GET` | None | `Map<String, Object>` | Predefined Hungarian cities, regional presets, tracked cities. |
| `/api/config` | `GET` | None | `SearchCriteria` | Retrieves last saved search criteria. |
| `/api/config` | `POST` | `SearchCriteria` (JSON) | `{"success": true}` | Persists user search criteria. |
| `/api/history` | `GET` | `city`, `portal`, `minPrice`, `maxPrice`, `minArea`, `maxArea`, `minRooms`, `maxRooms`, `excludeViewed`, `onlyDiscounted`, `sortBy`, `limit`, `offset` | `List<ListingDto>` | Multi-criteria filtered, paginated property search across database. |
| `/api/properties/price-history` | `GET` | `id` or `url` param | `List<PriceHistoryEntry>` | Price change history log for a specific property. |
| `/api/properties/{id}/price-history` | `GET` | `id` path variable | `List<PriceHistoryEntry>` | Path-variable variant for property price history. |
| `/api/properties/status-log` | `GET` | `id` or `url` param | `List<StatusLogEntry>` | Discovery and status audit trail for a property. |
| `/api/properties/{id}/status-log` | `GET` | `id` path variable | `List<StatusLogEntry>` | Path-variable variant for property status log. |
| `/api/properties/discounted` | `GET` | `city` (optional) | `List<IngatlanItem>` | All properties with recorded negative price deltas. |
| `/api/search` | `POST` | `SearchCriteria` (optional) | `DashboardResponseDto` | Executes parallel scraping, deduplication, filtering, and returns aggregated results. |
| `/api/open-urls` | `POST` | `List<String>` (URLs) | `{"success": true, "count": N}` | Marks listings as viewed; optionally opens in desktop browser if configured. |
| `/api/items/viewed` | `POST` | `{"url": "..."}` | `{"success": true, "marked": bool}` | Marks a single listing as viewed. |
| `/api/history/clear` | `POST` | `city` (optional body/query) | `{"success": true}` | Clears history for a specific city or entire database. |

### 3.3 Concurrency Model: Java 25 Virtual Threads
High-throughput parallel I/O is achieved without carrier thread saturation:
- Spring Boot web container uses Virtual Threads: `spring.threads.virtual.enabled=true`.
- Parallel scraping runs via `Executors.newVirtualThreadPerTaskExecutor()` in `ParallelPortalScrapeExecutor`.
- All network fetches have an explicit 15-second timeout (`Future.get(15, TimeUnit.SECONDS)`). Carrier threads are never blocked by slow network sockets.

### 3.4 0-Crash Resilience & Global Exception Handling
- `GlobalExceptionHandler` (`@RestControllerAdvice`) intercepts all uncaught exceptions, converting them into structured JSON error payloads with ISO timestamps, HTTP status codes, error messages, and request paths.
- Portal scrapers wrap network and parsing failures inside `PortalScrapeResult.failure(...)`. A failure on one portal never terminates other active scrapers or aborts the search process.

---

## 4. Persistence Architecture & Dual-Mode Strategy

### 4.1 Storage Abstraction
Persistence is abstracted behind two clean interfaces in `ingatlanok.persistence`:
- `ListingStorage`: Querying, saving, deduplicating, price history tracking, status logging, and lifecycle operations.
- `ConfigStorage`: Persistence of user `SearchCriteria`.

`JpaIngatlanStorage` implements both contracts using standard Spring Data JPA repositories.

### 4.2 Dual-Mode Persistence Strategy

| Mode | Environment | Datasource URL | DDL Policy | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Development & CLI** | Local machine / Developer workstation | `jdbc:h2:file:./data/ingatlanok_jpa;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1` | `update` | Zero-configuration local development. Persists to `data/ingatlanok_jpa.mv.db`. Auto-server allows external H2 console access. |
| **Production** | Multi-container Docker Compose | `jdbc:postgresql://db:5432/${POSTGRES_DB:-ingatlandb}` | `update` | Robust, enterprise-grade concurrent storage on PostgreSQL 17 Alpine container with persistent named volume `postgres_data`. |
| **Hermetic Testing** | Offline JUnit 5 test runs (`./gradlew check`) | `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | `create-drop` | 100% ephemeral, isolated, deterministic in-memory database with zero disk pollution. |

### 4.3 Entity Model & $O(1)$ Deduplication
- `PropertyEntity`: Core entity mapped to `properties` table.
  - Indexed columns:
    - `idx_properties_url` on `url` (Unique B-Tree index): Enables $O(1)$ deduplication and status lookup.
    - `idx_properties_city_key` on `normalizedCity`: Accelerates city-level queries.
    - `idx_properties_discovered` on `discoveredAt`: Enables efficient chronological sorting.
- `PriceHistoryEntity`: Mapped to `price_history` table, tracking all historical price fluctuations with delta amount and percentage.
- `StatusLogEntity`: Mapped to `status_log` table, recording listing state changes (`ACTIVE`, `VIEWED`, `INACTIVE`).
- `ConfigEntity`: Mapped to `app_config` table, storing key-value pairs and serialized criteria.

### 4.4 Retirement of SQLite and Nitrite
- **SQLite Removal**: Previously considered for local storage with WAL mode. Removed in favor of Spring Data JPA on embedded H2, which delivers full JPA/Hibernate compatibility, identical SQL semantics between development and PostgreSQL production, and zero native C-library dependencies.
- **Nitrite NoSQL Removal**: Decommissioned due to MVStore file lock concurrency bugs during parallel test runs and lack of relational schema safety.

---

## 5. Web Scraping & Pluggable Portal Engine Architecture (Layer 3)

### 5.1 Pluggable Engine Interface & Adapter Architecture
Every portal scraper implements `PortalEngine`, bridged to uniform extraction adapters:
```java
public interface PortalEngine {
    PortalDescriptor getDescriptor();
    PortalScrapeResult scrape(SearchCriteria criteria);
}
```
All engines register dynamically in `PortalEngineRegistry`. Under the hood, portál implementations delegate to `AdapterPortalEngine` backed by `PortalAdapter` and ordered `ExtractionStrategy` pipelines (`API`, `STATE_JSON`, `JSON_LD`, `HTML`, `SIDECAR`), feeding raw extraction results (`RawPropertyListing`) into the centralized `ListingNormalizer` and `ListingValidator` (Layer 2a).

### 5.2 Portal Inventory & Extraction Strategies

| Portal | Domain | Method | Extraction Strategy & Anti-Bot Mitigations |
| :--- | :--- | :--- | :--- |
| **Duna House** | `dh.hu` | Live Jsoup DOM | Pattern: `/elado-ingatlan/<type>/<county>/<city>/<price>-mFt`. Adapter with `DunaHouseHtmlStrategy` parsing into `RawPropertyListing`. |
| **Otthon Centrum** | `oc.hu` | Live Jsoup DOM | Rich card parsing with `OtthonCentrumHtmlStrategy` using Chrome 133 `Sec-Ch-Ua` headers and client hints to prevent bot challenges. |
| **Jófogás Ingatlan** | `jofogas.hu` | Live Jsoup DOM | Search query parsing with `JofogasHtmlStrategy` producing `RawPropertyListing`. |
| **Költözzbe** | `koltozzbe.hu` | Live DOM & JSON-LD | Dual strategy: `KoltozzbeExtractionStrategy` parsing HTML cards and embedded Schema.org JSON-LD microdata into raw listings. |
| **Ingatlanbazár** | `ingatlanbazar.hu` | Live REST API | High-speed JSON endpoints (`/api/property-search`) handled by `IngatlanBazarApiStrategy` producing `RawPropertyListing` with typed HUF pricing. |
| **Ingatlan.com** | `ingatlan.com` | Sidecar / Graceful Fallback | Priority chain: `IngatlanComSidecarStrategy` first; fallback to `IngatlanComHtmlStrategy` (Cloudflare WAF bot detection) returning `BOT_PROTECTED` status card without hanging. |

### 5.3 Anti-Bot & Browser Client Hints
`HttpFetcher` standardizes all outbound requests:
- User-Agent: Chrome 133 on Windows 10/11 64-bit.
- Client Hints: `sec-ch-ua`, `sec-ch-ua-mobile: ?0`, `sec-ch-ua-platform: "Windows"`.
- Fetch Metadata: `sec-fetch-dest`, `sec-fetch-mode`, `sec-fetch-site`, `sec-fetch-user: ?1`.
- Timeout: 15,000 ms strictly enforced.

---

## 6. Frontend Architecture (React 19 SPA)

### 6.1 Client Technology Stack
- **Framework**: React 19.0.0 (Functional components, React Hooks).
- **Language**: TypeScript 5.9.3 (Strict type checking, `noEmit: true`).
- **Styling**: Tailwind CSS v4.3.3 with PostCSS and custom dark/light theme tokens.
- **Icons & Charts**: `lucide-react` (1.16.0), `recharts` (2.15.1).
- **Build Tool**: Vite 8.3.4 with `@vitejs/plugin-react` and `vite-plugin-compression2` (Gzip pre-compression).
- **Testing**: Vitest 5.0.4 with JSDOM and Playwright 1.64.1 for E2E.

### 6.2 State Management Architecture
- **Client & Filter State (Zustand 5.0.3)**: Manages local filter settings, active cities, price/area sliders, view mode (grid/list), and theme selection (dark/light).
- **Server State & Caching (TanStack Query 5.67.1)**: Manages API communication (`/api/status`, `/api/portals`, `/api/cities`, `/api/history`, `/api/search`), request deduplication, loading states, and query invalidation.

### 6.3 Typography & Online Google Fonts CDN
Typography is standardized on **Plus Jakarta Sans**, loaded via Google Fonts CDN in `frontend/index.html`:
```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
```

### 6.4 Decoupled Serving Model
- **Development**: Vite dev server runs at `http://localhost:5173`. Its built-in proxy routes all `/api` requests to `http://localhost:8080`.
- **Production**: Nginx reverse proxy serves static assets from `/usr/share/nginx/html` and proxies `/api` requests to `http://backend:8080`.

---

## 7. Application Security (AppSec) & Server Hardening

1. **Localhost Default Binding**: Spring Boot binds by default to `127.0.0.1` (`server.address: ${SERVER_HOST:127.0.0.1}`). In containerized environments, `SERVER_HOST=0.0.0.0` is explicitly injected via Docker Compose.
2. **Global CORS Filter (`GlobalCorsFilter`)**: Registered with `@Order(Ordered.HIGHEST_PRECEDENCE)`. Restricts origins to localhost and loopback interfaces (`localhost`, `127.0.0.1`, `0.0.0.0`, `[::1]`) across all ports. Preflight `OPTIONS` requests from unauthorized origins return `403 Forbidden`.
3. **Security Headers**: Injected on every response:
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY`
   - `X-XSS-Protection: 1; mode=block`
   - `Referrer-Policy: strict-origin-when-cross-origin`
4. **Content Security Policy (CSP)**:
   - Nginx and Spring Boot define strict CSP rules allowing `'self'`, Google Fonts CDN (`fonts.googleapis.com`, `fonts.gstatic.com`), and localhost API connectivity.
5. **SSRF Mitigation**: Web scraping engines are restricted to known portal domain endpoints (`dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, `ingatlan.com`). Outbound URL construction validates host domains.
6. **Input Sanitization**: Price strings, room counts, and square meters are parsed using defensive regex extractors in `PropertyParser`, preventing injection and unhandled formatting exceptions.

---

## 8. Standard JVM Runtime

The backend runs as a standard JVM application packaged as an executable Spring Boot JAR.

---

## 9. Containerization & Production Deployment Topography

### 9.1 Multi-Stage Hardened Dockerfile
Backend containerization uses Eclipse Temurin JRE 25 with non-root security context:
- Stage 1 (`builder`): Compiles fat JAR using `eclipse-temurin:25-jdk-alpine` and `./gradlew bootJar -x test`.
- Stage 2 (`runtime`): Uses `eclipse-temurin:25-jre-alpine`, creates system user `appuser` (UID 10001, GID 10001), installs `curl` for container healthchecks, and executes `app.jar` as non-root.

### 9.2 Production 3-Tier Docker Compose Stack
```text
┌─────────────────────────────────────────────────────────────┐
│                      Docker Host Network                    │
│                                                             │
│   Port 8080 (HOST_PORT)                                     │
│       │                                                     │
│       ▼                                                     │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  Frontend Service: Nginx 1.27 Alpine (Port 80)      │   │
│   │  - Serves compiled React 19 SPA static assets       │   │
│   │  - Gzip compression, immutable cache headers        │   │
│   │  - Reverse-proxies /api to http://backend:8080      │   │
│   └─────────────────────────┬───────────────────────────┘   │
│                             │                               │
│                             ▼                               │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  Backend Service: Headless Spring Boot 4.1.1 (8080) │   │
│   │  - Temurin JRE 25, UID 10001 non-root              │   │
│   │  - Virtual Threads, REST API (/api/*)              │   │
│   │  - Healthcheck: curl -f http://localhost:8080/api/status│
│   └─────────────────────────┬───────────────────────────┘   │
│                             │                               │
│                             ▼                               │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  Database Service: PostgreSQL 17 Alpine (Port 5432) │   │
│   │  - Persistent named volume: postgres_data           │   │
│   │  - Healthcheck: pg_isready -U postgres -d ingatlandb│   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 9.3 Service Healthcheck & Startup Chaining
1. `db` starts and verifies health via `pg_isready`.
2. `backend` starts only when `db` is healthy (`depends_on: db: condition: service_healthy`) and connects to `jdbc:postgresql://db:5432/...`.
3. `frontend` starts only when `backend` is healthy (`depends_on: backend: condition: service_healthy`) and begins serving the SPA and reverse-proxying API traffic.

### 9.4 Clean Numeric Versioning
- Docker Release calculates a Semantic Version from the latest Git release tag and a `patch`, `minor`, `major`, or custom version input. Docker builds receive that version as a build argument; the Gradle project version does not select the release version.
- Container images published to GHCR strictly emit clean numeric tags without `v` prefix (`1.0.0`, `1.0.0-<sha>`).

---

## 10. Quality Assurance & Hermetic Verification

1. **100% Offline Hermetic Testing**: Automated unit and integration tests run entirely offline (`./gradlew test`). Live network scraping is forbidden in standard test cycles.
2. **JaCoCo Quality Gate**: Instruction and line coverage must exceed 80% across core, persistence, and domain packages. Missing coverage reports or coverage under 80% fails the CI pipeline.
3. **Checkstyle Static Analysis**: Checkstyle 10.21.4 verifies compliance with `config/checkstyle/checkstyle.xml` (0 warnings permitted).
4. **Frontend Verification**: TypeScript compilation (`npm run typecheck`), ESLint (`npm run lint`), and Vitest unit tests (`npm test`) run in CI.

---

## 11. AI Governance & Specialist Skills Review Protocol

### 11.1 The 3 Specialist Skills
To eliminate conflicting instructions and ensure strict separation of concerns, AI code generation and PR reviews are governed by 3 specialist personas defined in `.agents/skills/`:
1. `fullstack-architect`: System architecture, headless REST contract, Virtual Threads concurrency, Spring Data JPA persistence, React 19 UI integrity.
2. `scraper-security-engineer`: Portal scrapers, DOM/JSON-LD parsing, Cloudflare/WAF anti-bot resilience, 0-crash error handling, AppSec (CORS, CSP, localhost binding, SSRF, input sanitization).
3. `quality-devops-engineer`: Offline hermetic test suites, JaCoCo coverage thresholds, Checkstyle, Dockerfile and Docker Compose hardening, and the `CI Build`, `PR build`, `Docker Release`, and `Docker Build` workflows.

### 11.2 Standard Decision Semantics
Specialist reviews emit standardized decision tokens:
- `NO_FINDING`: Domain verified; changes comply fully; 0 defects found.
- `FINDING`: Architectural violations, bugs, regressions, or security risks identified.
- `NOT_APPLICABLE`: Scope of changes does not touch this domain.
- `NOT_VERIFIED`: Domain is touched or relevant, but could not be verified. Gate must fail-closed.

Specialist decision tokens are used in manual reviews; they are not a required PR description gate.

---

## 12. Architectural Discrepancy Matrix & Retirement Log

| Component / Technology | Original / Historical State | Final Architecture State | Rationalization & Motivation |
| :--- | :--- | :--- | :--- |
| **SQLite (WAL mode)** | SQLite via JDBC with WAL mode and HikariCP | **Spring Data JPA on Embedded H2 (dev) / PostgreSQL 17 (prod)** | Eliminated native C-library dependencies, aligned SQL semantics between dev and prod, enabled native JPA repositories. |
| **Nitrite NoSQL DB** | Nitrite document store (`data/ingatlanok_nosql.db`) | **Completely Removed** | Eliminated file locking bugs in parallel tests and lack of transactional relational safety. |
| **Maven (`mvn`, `pom.xml`)** | Mentioned in legacy commands and scripts | **Gradle 9.7.1 Kotlin DSL (`build.gradle.kts`)** | Single unified build tool with fast incremental builds. |
| **Monolithic Static Bundle** | HTML/JS packaged in `src/main/resources/static/` | **Autonomous React 19 SPA served via Vite (dev) or Nginx (prod)** | True decoupling of frontend and backend. Enables independent scaling, caching, and modern web development workflow. |
| **`WebDashboardHtml.java`** | Monolithic Java class generating inline HTML | **Completely Removed** | Replaced entirely by modern React 19 + TypeScript + Tailwind SPA. |
| **Zero CDN Rule** | "Zero external CDN dependencies" rule in old instructions | **Online Google Fonts CDN allowed for typography** | High-performance font delivery (Plus Jakarta Sans) without bloating repository assets. |
| **Local Machine Links** | Local file URIs (`file` scheme) in pull request template | **Relative repository markdown links** | Ensures links work seamlessly on GitHub web interface. |
