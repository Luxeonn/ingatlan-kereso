---
name: fullstack-architect
description: Full-stack software architecture and product engineering specialist. Use this skill for the Java 21 backend (Spring Boot 4.1.0, Spring Framework 7.0.9, virtual threads, and a headless Spring MVC REST API), persistence (Spring Data JPA, Hibernate, embedded H2, and PostgreSQL 17), and the modern React 19 + TypeScript + Tailwind SPA frontend, including UI/UX micro-interactions and type-safe UI integrity.
---

# Fullstack Architect Specialist

This specialist skill owns **system architecture**, **layer-boundary integrity**, the **storage and persistence layer**, and the **standalone React 19 web user interface**.

---

## 1. Purpose & Activation

### Primary Mission

Maintain the application's layered, loosely coupled architecture; correct Java 21 virtual-thread concurrency; a clean headless REST API contract; type-safe, dialect-independent Spring Data JPA persistence; and high-quality React 19 + TypeScript frontend integrity.

### Activation Triggers

Activate this skill for any task, pull request, or code review affecting any of the following:

- **Backend architecture and layers**: `src/main/java/ingatlanok/(core|model|persistence|presentation|web)/`
- **Application entry points and configuration**: `src/main/java/ingatlanok/IngatlanApplication.java`, `src/main/java/ingatlanok/Main.java`, `src/main/resources/application.yml`
- **Frontend SPA client**: `frontend/**` (`src/`, `package.json`, `vite.config.ts`, `nginx.conf`, `index.html`)
- **Build and architecture-level dependencies**: `build.gradle.kts`, `settings.gradle.kts`
- **PR labels**: `architecture`, `enhancement`, `frontend`, `backend`

---

## 2. Scope & Non-Goals

### In-Scope Responsibilities

1. **Four-layer unidirectional architecture**:
   - `web/`: Spring MVC `@RestController` headless endpoints (`/api/search`, `/api/stats`, `/api/portal-statuses`, `/api/config`, `/api/status`), CORS, and web configuration (`SpaWebMvcConfig`, `GlobalCorsFilter`).
   - `presentation/`: DTO models, KPI aggregation, and aggregated market statistics.
   - `core/`: Filtering engine (`PropertyFilterEngine`), O(1) deduplication (`DeduplicationEngine`), aggregation, and parallel query coordination (`SearchExecutionEngine`, `MarketAnalyticsEngine`).
   - `persistence/`: Storage abstractions (`ListingStorage`, `ConfigStorage`), Spring Data JPA entities (`PropertyEntity`, `PriceHistoryEntity`, `ConfigEntity`), repositories (`PropertyRepository`), and the `JpaIngatlanStorage` implementation.
2. **Concurrency model (Java 21 LTS)**:
   - Virtual threads (`spring.threads.virtual.enabled=true`, `Executors.newVirtualThreadPerTaskExecutor()`). Avoid blocking carrier threads.
3. **Persistence strategy (dual-mode JPA)**:
   - Embedded file-based H2 storage (`jdbc:h2:file:./data/ingatlanok_jpa;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`) in CLI/development mode.
   - PostgreSQL 17 in the production Docker environment.
   - Keep persistence decoupled: direct JDBC calls and obsolete embedded or NoSQL engines are prohibited.
4. **Modern standalone React 19 frontend**:
   - React 19, TypeScript 5.9, Tailwind CSS v4, and Vite 8.3.
   - Client state management (Zustand) and server-state synchronization (TanStack Query).
   - Responsive layout (360px through 4K), light/dark themes, skeleton loading, and Google Fonts CDN typography (Plus Jakarta Sans).

### Explicit Non-Goals

- **Web scraping and portal-specific DOM analysis**: Internal portal-engine logic, Jsoup selectors, and Cloudflare WAF handling belong to `scraper-security-engineer`.
- **CI/CD workflows, container hardening, and JaCoCo calculations**: GitHub Actions (`ci.yml`, `pr-build.yml`, `release.yml`, `images.yml`), multi-stage Dockerfile optimization, and coverage gates belong to `quality-devops-engineer`.

---

## 3. Required Workflow

For every review, the specialist must follow this four-phase workflow:

```
┌─────────────────────────────────┐
│ 1. Context Discovery & Audit    │ ➔ File inventory, layer classification, change scope
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 2. Architecture Validation      │ ➔ Layer boundaries, storage abstraction, virtual threads
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 3. Build & Type Validation      │ ➔ ./gradlew compileJava, npm run typecheck
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 4. Synthesis & Decision         │ ➔ Thematic gate status (NO_FINDING / FINDING)
└─────────────────────────────────┘
```

1. **Phase 1: Context discovery and file audit**
   - Identify the affected files.
   - Check whether the change affects layer boundaries (for example, Web -> Core -> Persistence) or the frontend/backend REST contract.
2. **Phase 2: Architecture and rule validation**
   - Verify that no layer inversion exists (for example, `core` must not reference `web` or `presentation` classes).
   - Verify that database operations go exclusively through the `ListingStorage` and `ConfigStorage` interfaces.
   - Check for synchronized blocks (`synchronized`) around long-running I/O in virtual-thread code.
3. **Phase 3: Empirical and build verification**
   - Verify backend compilation with `./gradlew compileJava`.
   - Verify frontend type integrity with `cd frontend && npm run typecheck` (or `npx tsc --noEmit`).
   - Check for linting or syntax errors.
4. **Phase 4: Synthesis and decision**
   - Determine the decision token (`NO_FINDING`, `FINDING`, `NOT_APPLICABLE`, or `NOT_VERIFIED`).
   - Provide exactly one concise, professional rationale sentence for the PR gate.

---

## 4. Review Checklist

### A. System Architecture and Layer Boundaries

- [ ] Dependencies between the four layers flow strictly downward: `web` -> `presentation` -> `core` -> `persistence` / `portal`.
- [ ] Layer inversion is prohibited: lower layers must not import classes from higher layers.
- [ ] The Spring Boot application is a fully headless REST API (`spring.web.resources.add-mappings: false`) and does not serve embedded static HTML from the backend JAR.
- [ ] REST endpoint URLs and JSON DTO contracts remain consistent with the frontend client.

### B. Virtual Threads and Concurrency

- [ ] Virtual threads are enabled (`spring.threads.virtual.enabled=true`).
- [ ] No calls block carrier threads (for example, synchronized blocks around I/O or `ThreadLocal` memory leaks).
- [ ] Parallel scraping and aggregation use `Executors.newVirtualThreadPerTaskExecutor()` or a Spring asynchronous mechanism.

### C. Persistence and Spring Data JPA

- [ ] The persistence layer uses Spring Data JPA entities and repositories exclusively.
- [ ] All persistence access goes through the `ListingStorage` or `ConfigStorage` interfaces.
- [ ] No direct JDBC/SQL calls exist in business logic.
- [ ] No obsolete persistence engines or unsupported database drivers are present in the codebase.
- [ ] Indexes on entities (`PropertyEntity`, `PriceHistoryEntity`) support O(1) deduplication and fast queries.

### D. Frontend Integrity and UI/UX

- [ ] The frontend is type-safe (`tsc --noEmit` completes with zero errors).
- [ ] Components use standard React 19 hooks, with no memory leaks or unnecessary re-renders.
- [ ] Server state is managed with TanStack Query and appropriate error and loading-skeleton states.
- [ ] The UI is fully responsive on mobile (360px) and desktop (4K) viewports.

---

## 5. Evidence & Output

The specialist must record the following concrete evidence during review:

- **Architecture review findings**: List affected interfaces and classes with exact file and line references.
- **Compilation and type-check output**: Record the results of `./gradlew compileJava` and `npm run typecheck`.
- **REST contract verification**: Document unchanged or updated DTO structures.

### Thematic Agent Approval Gate Output

Use the following text in the Pull Request's Thematic Gate block:

```markdown
- [x] Fullstack Architecture & UI (`fullstack-architect`): [NO_FINDING / NOT_APPLICABLE] - [One concise sentence summarizing architecture integrity and UI type safety]
```

*Note: For `FINDING`, leave the checkbox unchecked (`[ ]`), set the status to `[FINDING]`, and attach a detailed report in the PR description or a comment.*

---

## 6. Standard Decision Semantics

- **`FINDING`**: Use when layer inversion, carrier-thread blocking, direct database access from core, unsupported persistence technology, or a TypeScript compilation error is found.
  - *Required format*: `FINDING: [Severity: CRITICAL/MAJOR/MINOR] - [File:Line] - [Description and remediation guidance]`
- **`NO_FINDING`**: The architecture, persistence, and frontend layers meet the requirements with zero errors and zero warnings.
- **`NOT_APPLICABLE`**: The task only affects scraper engines or CI/CD configuration and does not change backend architecture, the REST API, data model, or frontend.
- **`NOT_VERIFIED`**: Compilation or type checking cannot be executed (for example, because Node/npm or Gradle is unavailable). As a fail-closed safeguard, the PR must not be approved.

---

## 7. Stop Conditions

The specialist must stop immediately and reject approval with a `FINDING` status if any of the following conditions occurs:

1. **Layer-boundary violation (layer inversion)**: `core`, `persistence`, or `portal` directly references classes in `web` or `presentation`.
2. **Virtual-thread blocking**: A synchronized block or blocking call is used around long-running I/O.
3. **Reintroduction of obsolete database technology**: Unapproved database drivers or obsolete embedded storage libraries appear in dependencies or source code.
4. **Bypassing storage abstraction**: Direct SQL or DataSource access occurs in web controllers or core modules.
5. **Frontend type or compilation error**: `tsc --noEmit` reports an error, or React components contain syntax errors.
6. **Serving static files from the backend**: HTML/JS/CSS is packaged into the Spring Boot JAR instead of being served by the independent frontend container.

---

## 8. Completion Criteria

Approval (`NO_FINDING`) from `fullstack-architect` may be granted only when ALL of the following criteria are met:

1. The backend source compiles successfully: `./gradlew compileJava` exits successfully.
2. Frontend type checking succeeds: `cd frontend && npm run typecheck` completes with zero errors.
3. Layered unidirectional dependencies are clean, with no layer inversion.
4. Spring Data JPA entities and repositories meet the specification.
5. REST API endpoint contracts and DTO structures match the frontend client.
6. The PR Gate block contains exactly one professional, non-generic rationale sentence.
