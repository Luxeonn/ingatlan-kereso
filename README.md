# Ingatlan Kereső

A real estate search and monitoring app for Hungarian property portals. The Java 25 / Spring Boot backend collects and deduplicates listings; a React and TypeScript frontend provides search, filters, and market summaries.

## Supported portals

Duna House, Otthon Centrum, Jófogás, Költözzbe, Ingatlanbazár, and Ingatlan.com. Ingatlan.com currently uses a fallback because the portal restricts automated access.

## Run locally

Requirements: JDK 25, Node.js 22, and npm.

Start the backend from the repository root:

```bash
./gradlew bootRun
```

On Windows, use `gradlew.bat bootRun`. The API is available at [http://localhost:8080/api/status](http://localhost:8080/api/status).

In another terminal, start the frontend:

```bash
cd frontend
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies API requests to the local backend.

## Run with Docker Compose

Install and start Docker Desktop, then choose one of these modes from the repository root.

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

The second mode pulls `ghcr.io/luxeonn/ingatlan-kereso-backend:latest` and `ghcr.io/luxeonn/ingatlan-kereso-frontend:latest` by default. Image names can be overridden with `BACKEND_IMAGE` and `FRONTEND_IMAGE` in a local `.env` file.

The `Docker Build` workflow updates these `latest` images after pushes to `develop`. To publish a versioned release, run `Docker Release` manually from `main` and select a `patch`, `minor`, `major`, or custom SemVer version. It publishes both GHCR images before creating the matching `vX.Y.Z` Git tag and GitHub Release.

On Windows, `scripts/start-docker.bat` presents both modes. Open [http://localhost:8080](http://localhost:8080) when the containers are healthy. Use `docker compose logs -f backend` to follow logs and `docker compose down` to stop the stack; this keeps the database volume.

## Checks

```bash
./gradlew check bootJar
cd frontend
npm run lint
npm run typecheck
npm run test
npm run build
npm run test:e2e
```

The backend uses JUnit 5 and Checkstyle; frontend unit tests use Vitest and browser tests use Playwright.

## Project resources

- [Architecture](docs/architecture.md)
- [REST API specification](openapi/api-spec.yaml)
- [Security policy](SECURITY.md)
- [Contributor guide](AGENTS.md)
- [License](LICENSE) (GPL-3.0)
