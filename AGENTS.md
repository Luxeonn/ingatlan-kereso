# Repository Guidelines

## Project Structure & Modules

The backend is a Java 25 Spring Boot application. Production code lives in `src/main/java/ingatlanok/`, with business logic in `core/`, domain types in `model/`, persistence in `persistence/`, portal integrations in `portal/`, and HTTP endpoints in `web/`. Backend tests and fixtures are under `src/test/`. The React 19, TypeScript, and Vite frontend is in `frontend/src/`; unit tests are in `frontend/src/test/` and browser tests in `frontend/e2e/`. `openapi/api-spec.yaml` is the API contract; regenerate frontend API types with `npm run generate-api` from `frontend/`. See `docs/architecture.md` for layer boundaries.

## Build, Test, and Development

- `./gradlew bootRun` (Windows: `.\gradlew.bat bootRun`) starts the backend on port 8080.
- `./gradlew check bootJar` runs backend verification and packages the executable JAR.
- `cd frontend && npm ci && npm run dev` installs dependencies and starts Vite on port 5173.
- `cd frontend && npm run lint && npm run typecheck && npm run test && npm run build` runs frontend CI checks.
- `cd frontend && npm run test:e2e` runs Playwright browser tests.

Docker publication is handled by `.github/workflows/images.yml` (`Docker Build`), which runs on pushes to `develop`, and `.github/workflows/release.yml` (`Docker Release`), which is manually started from a branch (such as `main` or `develop`). Releases derive their version from the latest Git release tag and a patch/minor/major/custom input; GHCR publication must succeed before the Git tag and GitHub Release are created.

## Code Generation Conventions (Lombok & MapStruct)

- **Lombok:** use `@Getter`/`@Builder`/`@NoArgsConstructor` (and `@Setter` only where post-construction mutation is needed) across models, presentation DTOs, and JPA entities. Prefer `@Builder` (with `@Builder.Default` or constructor-level `@Builder` for normalizing constructors, and `toBuilder = true` where copy-updates are needed) over setter chains, and omit `@Setter` on read-only DTOs/value objects (`MarketStatsDto`, `PriceDropItemDto`, `PortalStatusDto`, `DashboardResponseDto`, `PortalSource`, `PriceInfo`, `PropertyDetails`). Do not use `@Data`, and do not generate `equals`/`hashCode`/`toString` on JPA entities. Keep hand-written `equals`/`hashCode` where they encode business identity.
- **MapStruct:** use it for entity ↔ DTO/domain mappings (`JpaEntityMapper`, `PresentationMapper`) with `componentModel = "spring"` and `unmappedTargetPolicy = ReportingPolicy.ERROR`. Use `@AfterMapping` and `@MappingTarget` where post-mapping enrichment or merge semantics apply (`toItem`, `toProperty`, `updateEntityFromItem`), keeping only genuine stateful merge orchestration in Java.
- **Records:** prefer `record` for immutable value carriers; classes with `@Builder` remain where Jackson/JPA or custom normalizing constructors apply.

## Style & Naming

Follow the existing Java package and class structure. Checkstyle 10.21.4 is strict: use spaces (no tabs), keep Java lines within 120 characters, group and sort imports, and avoid wildcard imports, `System.out`/`System.err`, and `printStackTrace()`. Use descriptive PascalCase Java type names and camelCase members. Frontend components use `.tsx` with PascalCase names; hooks use `use`-prefixed camelCase. Keep generated API types generated from the OpenAPI spec rather than editing them by hand.

## Testing

Add JUnit 5 tests under the matching `src/test/java` package and Vitest tests in `frontend/src/test/` (typically `*.test.ts` or `*.test.tsx`). Add Playwright coverage in `frontend/e2e/` for user flows. Backend `check` excludes `live` tests; CI requires at least 80% Java line coverage. Keep ordinary tests deterministic and offline.

## Commits & Pull Requests

Request GitHub Copilot as reviewer on every PR you create (`gh pr edit <id> --add-reviewer "@copilot"`). Agents must always set Copilot as a reviewer.

Use Conventional Commit subjects, such as `feat(search): add area filter`, `fix(portal): handle missing prices`, or `docs: clarify setup`. Use a descriptive branch prefix such as `feature/`, `fix/`, or `docs/`; target `develop` by default. PRs must use concise, high-signal Hungarian descriptions adhering strictly to the template's 4 core sections (Context/Goal, Key changes, Verification, and Thematic Agent Approval Gate with 1-sentence rationales), link related issues with closing keywords (`Closes #<id>`), and strictly enforce clean UTF-8 encoding (`gh pr create --body-file ...`) without BOM to prevent mojibake. All required CI checks must pass before merge.

## Issue Creation & Tracking Guidelines

When creating GitHub issues (manually or via `gh issue create`), always use the formal templates located in `.github/ISSUE_TEMPLATE/` as the single source of truth:
- **Bugs:** Follow `.github/ISSUE_TEMPLATE/bug_report.yml` (`fix: <description>` title, `bug` label, problem description, expected vs actual behavior, reproduction steps, affected component, environment, logs).
- **Features / Enhancements:** Follow `.github/ISSUE_TEMPLATE/feature_request.yml` (`feat: <description>` title, `enhancement` label, user need / problem, proposed solution, affected specialist component / agent, alternatives).
- For automated/CLI creation, generate markdown adhering directly to the YAML schema's required sections to ensure uniform structure and high signal across all issues.


## Efficient Issue Implementation & CI Validation

- Before a broad change, turn every issue acceptance criterion into a checklist and map it to the affected code, API contract, tests, and CI/security gates. Inspect the relevant workflow files and existing validation scripts before implementation so requirements such as Checkstyle, JaCoCo, OpenAPI generation, and CodeQL are known up front.
- Implement the complete change in coherent, reviewable slices. Keep related production changes, compatibility updates, and their tests together; avoid publishing one-assertion test commits, syntax/style fixups, or successive trial-and-error security patches.
- Validate locally before the first push. For backend changes run `./gradlew check bootJar`; for frontend changes run `npm run lint`, `npm run typecheck`, `npm run test`, and `npm run build` from `frontend/`. For cross-stack work, run both suites and any affected contract/security checks. Resolve all findings from that pass together before pushing.
- For logic changes, add happy-path and boundary/error-path tests in the same implementation slice. Keep Java line coverage safely above the repository's 80% CI minimum (target at least 82%) so later small edits do not make the PR fragile.
- For security-sensitive networking, identify the tainted input and the final network sink before coding; use the shared validated `HttpFetcher` path where applicable. Review the repository's CodeQL rules and make validation explicit at the sink when static analysis cannot recognize an indirect sanitizer. Do not rely on repeated remote CodeQL failures to discover the required pattern.
- After pushing, verify required checks against the PR's current head commit and inspect the failed job's steps/logs before changing code. Batch all fixes revealed by a run into one local correction, rerun the relevant local checks, then push once. Do not push a new commit merely to retrigger checks that already passed.
- Prefer a small number of Conventional Commits, each representing a complete logical increment. Keep temporary checkpoints local; publish a commit only when its state is coherent and validated. A wide refactor can still use multiple commits when each is independently meaningful, but do not split mechanical fixups into separate remote commits.

## Configuration & Security

Use `.env.example` as the configuration reference; never commit secrets or real credentials. Development uses embedded H2, while Docker Compose configures PostgreSQL. Review `SECURITY.md` before reporting vulnerabilities.
