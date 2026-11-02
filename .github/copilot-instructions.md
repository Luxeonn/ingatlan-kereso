# GitHub Copilot & AI Code Review Instructions for Ingatlan Kereső

This repository is **ingatlan-kereso**, a high-performance Hungarian real estate aggregator built on Java 25 LTS with Spring Boot 4.1.1, Spring Data JPA / H2, Virtual Threads, a pure headless JSON REST API, and an autonomous modern React 19 SPA frontend.

When generating code, reviewing Pull Requests, or proposing refactors, strictly adhere to the following architectural directives and coding standards:

---

## 1. System Architecture & 4-Layer Boundaries

Code is organized strictly into four decoupled layers. Cross-layer dependencies must follow unidirectional flow:

```
[Layer 1: Frontend SPA & REST API] (Decoupled React 19 SPA + Spring MVC Headless REST API)
             │
             ▼
[Layer 2: Core Domain & Business Logic] (FilterEngine, DeduplicationEngine, MarketAnalyticsEngine)
             │
             ▼
[Layer 3: Pluggable Portal Engines] (Duna House, Otthon Centrum, Jófogás, Költözzbe, Ingatlanbazár, Ingatlan.com)
             │
             ▼
[Layer 4: Decoupled Persistence] (ListingStorage, ConfigStorage, JpaIngatlanStorage: Embedded H2 dev / PostgreSQL 17 prod)
```

1. **No Layer Inversion:** Never import Layer 1 (Presentation/Web) into Layer 2, 3, or 4.
2. **Pluggable Portal Design (OCP):** All scraper engines implement `PortalEngine` and register dynamically in `PortalEngineRegistry`. Never hardcode portal-specific logic in core orchestrators.
3. **Decoupled Persistence:** Never invoke raw SQL or database connections directly from the Web or Portal layers. All persistence access must go through `ListingStorage` or `ConfigStorage` interfaces.

---

## 2. Concurrency & Performance (Java 25 LTS)

1. **Virtual Threads First:** Use Java 25 lightweight virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
2. **Never Block Carrier Threads:** Avoid synchronized blocks around long I/O operations. Use concurrent data structures (`ConcurrentHashMap`, `ReentrantReadWriteLock`).
3. **Decoupled JPA Persistence (Dual-Mode):** Primary persistence relies on Spring Data JPA through `JpaIngatlanStorage`. Development/CLI runs on embedded H2 (`jdbc:h2:file:./data/ingatlanok_jpa;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`), while production runs on PostgreSQL 17 in Docker Compose. Ephemeral in-memory H2 (`jdbc:h2:mem:testdb`) is used for hermetic tests. Storage abstraction contracts (`ListingStorage`, `ConfigStorage`) isolate business logic from database vendors.
4. **Lombok & MapStruct Code Generation:** Use Lombok `@Getter`, `@Builder`, and `@NoArgsConstructor` (and `@Setter` only where post-construction mutation is required) across domain models, presentation DTOs, and JPA entities. Prefer `@Builder` (with `@Builder.Default`, constructor-level `@Builder` for normalizing constructors, and `toBuilder = true` for copy-updates) over setter chains; omit `@Setter` on read-only DTOs/value objects (`MarketStatsDto`, `PriceDropItemDto`, `PortalStatusDto`, `DashboardResponseDto`, `PortalSource`, `PriceInfo`, `PropertyDetails`). Never use `@Data`, `@EqualsAndHashCode`, `@ToString`, or `@SneakyThrows` (enforced by Checkstyle), and never generate `equals`/`hashCode`/`toString` on JPA entities. Use MapStruct (`JpaEntityMapper`, `PresentationMapper`) with `componentModel = "spring"` and `unmappedTargetPolicy = ReportingPolicy.ERROR` (`@AfterMapping`, `@MappingTarget`) instead of manual DTO/entity field-copy code.

---

## 3. Resilience & 0-Crash Guarantee

1. **0-Crash Principle:** A scraper failure (network timeout, WAF trigger, bot challenge, DOM alteration) on one portal must NEVER crash the server or abort searches on other portals.
2. **Graceful Fallback:** Scrapers must return structured `PortalScrapeResult` with explicit statuses (`SUCCESS`, `EMPTY`, `BOT_PROTECTED`, `FAILED`) and a direct browser fallback search URL.
3. **Safe Number Parsing:** Always sanitize currency amounts, floor areas, and room numbers. Never throw unhandled `NumberFormatException` in user-facing endpoints.

---

## 4. Frontend & UI Integrity (React 19 + TypeScript + Vite)

1. **Decoupled Frontend Serving & Typography:** The React 19 SPA client is completely decoupled from the Spring Boot backend (`src/main/resources/static/` is not used; `spring.web.resources.add-mappings: false` is enforced). Production SPA static assets are served directly by Nginx (reverse-proxying `/api` to Spring Boot), and local development runs via Vite proxy (port 5173). Typography uses Google Fonts CDN (Plus Jakarta Sans) loaded online in `frontend/index.html`.
2. **Type Safety, OpenAPI Schema Derivation & Linting:** Strictly enforce `tsc --noEmit` and `eslint .`. Avoid `any` types and unsafe type assertions. Derive frontend DTO types (`types/property.ts`, `types/api.ts`, `types/portal.ts`) directly from OpenAPI-generated `generated-api.ts` (`components['schemas']`); never hand-duplicate DTO interfaces or leave dead/unreferenced API functions.
3. **State Management & Deduplicated UI Primitives:** Use Zustand for client UI and filter state (`SORT_OPTIONS` and `updateCriteria` in `useFilterStore`), and TanStack Query for server state caching and synchronization. Centralize shared domain calculations and portal metadata (`normalizePortalKey`, `PORTAL_CHIPS`, `clampQualityScore`, `getQualityConfig`, `getEffectivePrice`, `getEffectiveSqmPrice`, and `openExternalUrl` with `noopener,noreferrer`) in `frontend/src/utils/formatters.ts`. Reuse `NumericStepper` (`AreaStepper`, `PriceStepper`) and `useOutsideDismiss` for popovers/dropdowns, and avoid redundant pass-through wrapper components.

---

## 5. Testing & Hermetic Quality Gates

1. **100% Offline & Hermetic Tests:** Automated unit and integration tests must run without external network connectivity. Live web scraping during `./gradlew test` is strictly prohibited.
2. **Mocking & Fixtures:** Use pre-recorded offline HTML/JSON fixtures (`src/test/resources/`) and mock portal engines.
3. **Coverage Standard:** Maintain 80%+ JaCoCo instruction and line coverage across domain and persistence packages.
4. **Static Code Analysis:** Checkstyle is strictly enforced on `./gradlew check`.

---

## 6. Git & PR Conventions

1. **Conventional Commits:** PR titles and commits must follow `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`, `chore: ...`.
2. **Worktrees & Branches:** All development is isolated in git worktrees branching from `develop`. Branch naming follows `feature/<name>`, `fix/<name>`, `refactor/<name>`.
3. **Thematic Agent Approval Gate:** Every PR requires formal sign-off from designated specialist personas (`fullstack-architect`, `scraper-security-engineer`, `quality-devops-engineer`) using standardized decision semantics (`[NO_FINDING]`, `[FINDING]`, `[NOT_APPLICABLE]`, `[NOT_VERIFIED]`) and a mandatory 1-sentence rationale. See `docs/architecture.md` for the authoritative architectural specification.
