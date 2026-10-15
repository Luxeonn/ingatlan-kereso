# GitHub Copilot & AI Code Review Instructions for Ingatlan Kereső

This repository is **ingatlan-kereso**, a high-performance Hungarian real estate aggregator built on Java 21 LTS, SQLite WAL mode, and a modern embedded React 19 SPA frontend.

When generating code, reviewing Pull Requests, or proposing refactors, strictly adhere to the following architectural directives and coding standards:

---

## 1. System Architecture & 4-Layer Boundaries

Code is organized strictly into four decoupled layers. Cross-layer dependencies must follow unidirectional flow:

```
[Layer 1: Frontend SPA & Web API] (React 19 SPA + WebServer REST Endpoints)
             │
             ▼
[Layer 2: Core Domain & Business Logic] (FilterEngine, DeduplicationEngine, MarketAnalyticsEngine)
             │
             ▼
[Layer 3: Pluggable Portal Engines] (Duna House, Otthon Centrum, Jófogás, Költözzbe, Ingatlanbazár, Ingatlan.com)
             │
             ▼
[Layer 4: Decoupled Persistence] (ListingStorage, ConfigStorage, SqliteListingStorage with WAL mode)
```

1. **No Layer Inversion:** Never import Layer 1 (Presentation/Web) into Layer 2, 3, or 4.
2. **Pluggable Portal Design (OCP):** All scraper engines implement `PortalEngine` and register dynamically in `PortalEngineRegistry`. Never hardcode portal-specific logic in core orchestrators.
3. **Decoupled Persistence:** Never invoke raw SQL or database connections directly from the Web or Portal layers. All persistence access must go through `ListingStorage` or `ConfigStorage` interfaces.

---

## 2. Concurrency & Performance (Java 21 LTS)

1. **Virtual Threads First:** Use Java 21 lightweight virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
2. **Never Block Carrier Threads:** Avoid synchronized blocks around long I/O operations. Use concurrent data structures (`ConcurrentHashMap`, `ReentrantReadWriteLock`).
3. **SQLite Concurrency (WAL Mode):** SQLite runs in WAL mode (`journal_mode=WAL`, `busy_timeout=15000`). Read transactions never block write transactions. Connection pooling is managed via HikariCP.

---

## 3. Resilience & 0-Crash Guarantee

1. **0-Crash Principle:** A scraper failure (network timeout, WAF trigger, bot challenge, DOM alteration) on one portal must NEVER crash the server or abort searches on other portals.
2. **Graceful Fallback:** Scrapers must return structured `PortalScrapeResult` with explicit statuses (`SUCCESS`, `EMPTY`, `BOT_PROTECTED`, `FAILED`) and a direct browser fallback search URL.
3. **Safe Number Parsing:** Always sanitize currency amounts, floor areas, and room numbers. Never throw unhandled `NumberFormatException` in user-facing endpoints.

---

## 4. Frontend & UI Integrity (React 19 + TypeScript + Vite)

1. **Self-Contained Bundles:** Zero external CDN dependencies. All fonts, styles, scripts, and icons must be packaged into `src/main/resources/static/`.
2. **Type Safety & Linting:** Strictly enforce `tsc --noEmit` and `eslint .`. Avoid `any` types and unsafe type assertions.
3. **State Management:** Use Zustand for client UI and filter state, and TanStack Query for server state caching and synchronization.

---

## 5. Testing & Hermetic Quality Gates

1. **100% Offline & Hermetic Tests:** Automated unit and integration tests must run without external network connectivity. Live web scraping during `./gradlew test` is strictly prohibited.
2. **Mocking & Fixtures:** Use pre-recorded offline HTML/JSON fixtures (`src/test/resources/`) and mock portal engines.
3. **Coverage Standard:** Maintain 80%+ JaCoCo instruction and line coverage across domain and persistence packages.
4. **Static Code Analysis:** Checkstyle and SpotBugs are strictly enforced on `./gradlew check`.

---

## 6. Git & PR Conventions

1. **Conventional Commits:** PR titles and commits must follow `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`, `chore: ...`.
2. **Worktrees & Branches:** All development is isolated in git worktrees branching from `develop`. Branch naming follows `feature/<name>`, `fix/<name>`, `refactor/<name>`.
3. **Thematic Agent Approval Gate:** Every PR requires approval across architectural, security, database, frontend, workflow, and reliability domains.
