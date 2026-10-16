# Project: Code Slimming, God-Class Decomposition & Layered Architectural Refactoring

## Architecture
- **Target Repository**: `Luxeonn/ingatlan-kereso`
- **Working Branch**: `refactor/srp-decomposition-god-classes` (isolated git worktree)
- **Baseline Framework**: Java 21 LTS, Spring Boot 4.1.0, Spring Framework 7.0.8, Virtual Threads, Spring Data JPA / H2, React 19 SPA
- **Target Layered Architecture & Unidirectional Dependencies**:
  - `web` (REST controllers, CORS filter, SPA configuration)
    └──> `presentation` (DTOs, presentation facades, KPI formatters)
         └──> `core` (business engines: filtering, deduplication, search coordination)
              ├──> `model` (pure domain models & value objects: IngatlanItem, PriceInfo, SearchCriteria)
              ├──> `persistence` (ListingStorage, ConfigStorage, JpaIngatlanStorage, repositories) ──> `model`
              └──> `portal` (pluggable PortalEngines, portal descriptors, sidecar CLI) ──> `model`
  - `common` (leaf utilities: SlugUtils, HttpFetcher, CityUtils facade)
  - `scraper` (legacy backward-compatible adapter wrappers delegating to `portal`)

## Feature & Task Inventory
| # | Feature / Task | Description | Milestone | Source |
|---|----------------|-------------|-----------|--------|
| 1 | WebServer Slimming | Slim `WebServer.java` from 788 LOC to < 120 LOC delegating facade, retiring redundant HTTP handlers and static file streaming in favor of Spring MVC | M1 | Survey (R2, R3) |
| 2 | Legacy Scraper Consolidation | Consolidate 6 duplicate scrapers in `scraper` into thin forwarding wrappers delegating to `PortalEngineRegistry` | M1 | Survey (R3) |
| 3 | Main & Startup Streamlining | Remove dead `runWebDashboard()` and streamline `--web` Spring Boot delegation in `Main.java` and `IngatlanApplication.java` | M1 | Survey (R3) |
| 4 | Persistence Shared Logic Extraction | Extract shared in-memory filtering (`ListingFilterService`) and price tracking (`PriceHistoryTracker`, `StatusLogTracker`) | M2 | Survey (R2) |
| 5 | JpaIngatlanStorage Decomposition | Decompose `JpaIngatlanStorage.java` from 553 LOC to < 200 LOC with dedicated `JpaEntityMapper` | M2 | Survey (R2) |
| 6 | NitriteListingStorage Decomposition | Decompose `NitriteListingStorage.java` from 766 LOC to < 250 LOC with dedicated `NitriteDocumentMapper` | M2 | Survey (R2) |
| 7 | Eliminate DB Static Lock Defect | Resolve `data/ingatlanok_nosql.db` MVStore file locking in test runs caused by eager singletons | M2 | Survey (R2) |
| 8 | Dedicated JPA Storage Unit Tests | Add dedicated unit test suite for `JpaIngatlanStorage` and extracted persistence helpers | M2 | Survey (R2, AC) |
| 9 | SearchExecutionEngine Decomposition | Extract `ParallelPortalScrapeExecutor` and `SearchResultAggregator`, slimming engine from 267 LOC to < 120 LOC | M3 | Survey (R2) |
| 10 | CityUtils Decomposition | Extract `CityPresets`, `CountyDirectory`, and `CityLocationMatcher`, slimming `CityUtils.java` to < 60 LOC facade | M3 | Survey (R2) |
| 11 | Domain Model Decoupling | Move filtering methods out of `SearchCriteria` to `PropertyFilterEngine`; move Jsoup HTML parsing from `PropertyParser` to `portal` | M3 | Survey (R1, R2) |
| 12 | Portal ⇄ Scraper Cycle Elimination | Move `PortalScrapeResult`, `PortalScrapeStatus`, `SidecarCliExecutor`, `SidecarResponse` to `portal`/`model` with legacy aliases in `scraper` | M4 | Survey (R1) |
| 13 | Layer Boundary Inversion Fixes | Decouple `model.SearchResult` from `presentation.PortalStatusDto` and `core.MarketAnalyticsEngine` from `presentation.MarketStatsDto` | M4 | Survey (R1) |
| 14 | Package Dependency Verification | Validate clean unidirectional dependency flow with zero circular package dependencies | M4 | Survey (R1) |
| 15 | Full JUnit 5 Regression Verification | Verify 100% of test classes (221 tests) pass without regression (`./gradlew test --no-watch-fs`) | M5 | Survey (AC) |
| 16 | BootJar & Spring Context Validation | Verify `./gradlew bootJar` succeeds and Spring Boot context starts cleanly | M5 | Survey (AC) |
| 17 | Class Size & SRP Audit | Verify all refactored classes are strictly under 400 LOC (target < 200 LOC) | M5 | Survey (AC) |
| 18 | Adversarial Verification & Integrity Audit | Challenger empirical testing and Forensic Auditor verification | M5 | Project Pattern |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|--------------|--------|
| M1 | Legacy Infrastructure Consolidation | Slim `WebServer.java` (< 120 LOC), consolidate `scraper` adapters, streamline `Main.java` | None | PLANNED |
| M2 | Persistence God-Class Decomposition | Extract `ListingFilterService`, `PriceHistoryTracker`, `JpaEntityMapper`, decompose `JpaIngatlanStorage` (< 200 LOC) and `NitriteListingStorage` (< 250 LOC), fix DB lock | M1 | PLANNED |
| M3 | Core Engine & Utilities SRP Decomposition | Decompose `SearchExecutionEngine` (< 120 LOC), `CityUtils` (< 60 LOC), decouple `SearchCriteria` and `PropertyParser` | M2 | PLANNED |
| M4 | Layer Boundaries & Circular Dependency Elimination | Eliminate `portal` ⇄ `scraper` circularity, fix layer inversions (`model` -> `presentation`, `core` -> `presentation`) | M3 | PLANNED |
| M5 | Full Verification, Build, Adversarial & Integrity Gate | 100% test pass (221 tests), bootJar, class size audit (< 400 LOC), Challenger and Forensic Auditor gates | M4 | PLANNED |

## Interface Contracts
### Legacy WebServer ↔ Spring Boot MVC
- `WebServer.java` remains a minimal facade retaining public methods (`start()`, `stop()`, `getPort()`, `isRunning()`) so the 24 existing unit tests pass without modifying test assertions.
- All routing, HTTP handling, and static file serving is natively handled by Spring MVC (`RestApiController` and `SpaWebMvcConfig`).

### Persistence Abstraction ↔ Shared Helpers
- `ListingFilterService.filter(Collection<IngatlanItem>, ListingFilterDto)`: Stateless in-memory stream filtering and sorting.
- `PriceHistoryTracker.recordPriceChange(PropertyEntity/Document, PriceInfo)`: Encapsulates price delta math and status changes.
- `JpaEntityMapper`: Pure bidirectional mapping between `PropertyEntity` and `IngatlanItem`.

### Core Engine ↔ Helpers
- `ParallelPortalScrapeExecutor.executeScrapes(List<PortalEngine>, SearchCriteria, Duration)`: Encapsulates Virtual Threads and timeout resilience.
- `SearchResultAggregator.aggregate(List<PortalScrapeResult>)`: Encapsulates deduplication, sorting, and summary metrics.

## Code Layout
- Target branch/worktree: `refactor/srp-decomposition-god-classes` at `../worktrees/srp-decomposition-god-classes`
- New classes:
  - `ingatlanok/persistence/service/ListingFilterService.java`
  - `ingatlanok/persistence/service/PriceHistoryTracker.java`
  - `ingatlanok/persistence/service/StatusLogTracker.java`
  - `ingatlanok/persistence/mapper/JpaEntityMapper.java`
  - `ingatlanok/persistence/mapper/NitriteDocumentMapper.java`
  - `ingatlanok/core/ParallelPortalScrapeExecutor.java`
  - `ingatlanok/core/SearchResultAggregator.java`
  - `ingatlanok/common/city/CityPresets.java`
  - `ingatlanok/common/city/CountyDirectory.java`
  - `ingatlanok/common/city/CityLocationMatcher.java`
  - `ingatlanok/portal/model/PortalScrapeResult.java` (or moved from `scraper`)
