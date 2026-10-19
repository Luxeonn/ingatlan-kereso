# Repository Guidelines

## Project Structure & Modules

The backend is a Java 21 Spring Boot application. Production code lives in `src/main/java/ingatlanok/`, with business logic in `core/`, domain types in `model/`, persistence in `persistence/`, portal integrations in `portal/`, and HTTP endpoints in `web/`. Backend tests and fixtures are under `src/test/`. The React 19, TypeScript, and Vite frontend is in `frontend/src/`; unit tests are in `frontend/src/test/` and browser tests in `frontend/e2e/`. `openapi/api-spec.yaml` is the API contract; regenerate frontend API types with `npm run generate-api` from `frontend/`. See `docs/architecture.md` for layer boundaries.

## Build, Test, and Development

- `./gradlew bootRun` (Windows: `.\gradlew.bat bootRun`) starts the backend on port 8080.
- `./gradlew check bootJar` runs backend verification and packages the executable JAR.
- `./gradlew liveTest` runs portal tests tagged `live`; these require network access.
- `cd frontend && npm ci && npm run dev` installs dependencies and starts Vite on port 5173.
- `cd frontend && npm run lint && npm run typecheck && npm run test && npm run build` runs frontend CI checks.
- `cd frontend && npm run test:e2e` runs Playwright browser tests.

## Style & Naming

Follow the existing Java package and class structure. Checkstyle 10.21.4 is strict: use spaces (no tabs), keep Java lines within 120 characters, group and sort imports, and avoid wildcard imports, `System.out`/`System.err`, and `printStackTrace()`. Use descriptive PascalCase Java type names and camelCase members. Frontend components use `.tsx` with PascalCase names; hooks use `use`-prefixed camelCase. Keep generated API types generated from the OpenAPI spec rather than editing them by hand.

## Testing

Add JUnit 5 tests under the matching `src/test/java` package and Vitest tests in `frontend/src/test/` (typically `*.test.ts` or `*.test.tsx`). Add Playwright coverage in `frontend/e2e/` for user flows. Backend `check` excludes `live` tests; CI requires at least 80% Java line coverage. Keep ordinary tests deterministic and offline.

## Commits & Pull Requests

Use Conventional Commit subjects, such as `feat(search): add area filter`, `fix(portal): handle missing prices`, or `docs: clarify setup`. Use a descriptive branch prefix such as `feature/`, `fix/`, or `docs/`; target `develop` by default. PRs should explain context and technical changes, list validation commands, link related issues when applicable, and complete the template checklist. All required CI checks must pass before merge.

## Configuration & Security

Use `.env.example` as the configuration reference; never commit secrets or real credentials. Development uses embedded H2, while Docker Compose configures PostgreSQL. Review `SECURITY.md` before reporting vulnerabilities.
