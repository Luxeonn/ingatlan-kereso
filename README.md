# Ingatlan Kereső

A modern, resilient real estate search, market intelligence, and monitoring application for Hungarian property portals. Built with Java 25 LTS, Spring Boot 4.1.1, and React 19 / TypeScript / Tailwind CSS.

The application continuously aggregates listings across major portals, normalizes property types, deduplicates cross-portal listings into canonical properties, tracks historical time-series price movements, computes statistical median market metrics, and filters out data quality anomalies.

---

## Key Features

- **Multi-Portal Scraping**: Real-time extraction from Duna House, Otthon Centrum, Jófogás, Költözzbe, Ingatlanbazár, and Ingatlan.com (with fallback handling for bot protection).
- **Canonical Property Type Normalization**: Deterministic categorization into canonical types (`LAKAS`, `HAZ`, `TELEK`, `GARAZS`, `IPARI_KERESKEDELMI`, `EGYEB_ISMERETLEN`) with accessible, color-coded badges on property cards.
- **3-Tier Domain Architecture & Price History**: Clean domain and storage separation between physical properties (`Property`), portal source listings (`SourceListing`), and periodic time-series snapshots (`ListingObservation`), preserving detailed price drops and relisting events.
- **Cross-Portal Multi-Signal Deduplication**: Intelligently groups matching listings across different agencies into a single canonical property card displaying the lowest active market price, with an expandable multi-source comparison drawer listing all underlying portals, prices, and direct links.
- **Dedicated Market Analytics & Bargain Hunter**:
  - Autonomous Market Analysis panel with active-market **median Ft/m²** metrics (strictly separating residential living area from land/plot area).
  - Minimum sample size threshold protection (<5 listings indicates insufficient data rather than skewed statistics).
  - **"Alkalmi vétel" (Bargain)** badge for listings priced >= 20% below city+type reference median, featuring an interactive popover detailing sample count and reference benchmarks.
- **Rule-Based Data Quality Engine**: Deterministic 0–100 scoring engine with 10 evaluation rules (price sanity, area boundaries, realistic Ft/m², room consistency, extreme anomaly detection). Automatically excludes corrupt or low-quality listings (< 70 score) from market median calculations, and displays a 3-tier `<QualityScoreShield />` on property cards.
- **Boilerplate-Free & Contract-Driven Architecture**: Backend models, DTOs, and JPA entities use Lombok `@Builder`/`@Getter`/`@NoArgsConstructor` and compile-time MapStruct mappers (`JpaEntityMapper`, `PresentationMapper` with `ReportingPolicy.ERROR`), while the React 19 frontend derives its DTO types directly from the OpenAPI 3.1 schema (`generated-api.ts`) and shares centralized domain formatters (`utils/formatters.ts`) and UI primitives (`NumericStepper`, `useOutsideDismiss`).

---

## Supported portals

Duna House, Otthon Centrum, Jófogás, Költözzbe, Ingatlanbazár, and Ingatlan.com. Ingatlan.com currently uses a fallback strategy due to Cloudflare automated access restrictions.

---

## Run locally

**Requirements:** JDK 25, Node.js 22+, and npm.

### Backend

Start the headless Spring Boot service from the repository root:

```bash
# Linux / macOS
./gradlew bootRun

# Windows (PowerShell / CMD)
.\gradlew.bat bootRun
```

The REST API is available at [http://localhost:8080/api/status](http://localhost:8080/api/status).

### Frontend

In a separate terminal, start the Vite development server:

```bash
cd frontend
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The Vite dev proxy forwards `/api/*` requests to the local Spring Boot backend.

---

## Run with Docker Compose

Install and start Docker Desktop, then choose one of these modes from the repository root:

**Build images from the local project:**

```bash
docker compose build
docker compose up -d --no-build
```

**Download and run the latest images from GHCR:**

```bash
docker compose pull backend frontend
docker compose up -d --no-build
```

The stack starts PostgreSQL 17, the headless Spring Boot backend, and Nginx reverse proxy serving the React 19 SPA. Open [http://localhost:8080](http://localhost:8080) when the containers are healthy.

On Windows, `scripts/start-docker.bat` presents these options interactively (local build, GHCR latest, or a specific release version tag).

---

## Verification & Quality Gates

```bash
# Backend checks (Checkstyle, JUnit 5, JaCoCo line coverage >= 80%, bootJar)
./gradlew check bootJar

# Frontend checks (ESLint, TypeScript typecheck, Vitest, Vite build, Playwright E2E)
cd frontend
npm run lint
npm run typecheck
npm run test
npm run build
npm run test:e2e
```

---

## Project resources

- [Architecture Reference](docs/architecture.md)
- [REST API Specification (OpenAPI 3.1)](openapi/api-spec.yaml)
- [Security Policy](SECURITY.md)
- [Contributor & Agent Guidelines](AGENTS.md)
- [License](LICENSE) (GPL-3.0)
