# Ingatlan Kereső (Real Estate Scraper & Monitor)

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![GraalVM](https://img.shields.io/badge/GraalVM-Native%20Image-orange?style=for-the-badge&logo=graalvm&logoColor=white)](https://www.graalvm.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.12%2B-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)

High-performance real estate monitoring and scraping application built with Java 21 LTS, Spring Boot 4.1.0, GraalVM Native Image support, Virtual Threads, Spring Data JPA / H2 persistence, and a decoupled modern React 19 SPA dashboard.

---

## 📌 Overview

**ingatlan-kereso** automates property search across major Hungarian real estate portals. It aggregates active listings in a single unified view, deduplicates known ads, tracks newly discovered properties, and provides actionable market analytics.

* **Unified Multi-City Search:** Monitor multiple cities or predefined regional clusters (e.g., Budapest & Agglomeration, Debrecen area, University cities).
* **0-Crash Resilience:** Graceful degradation on anti-bot protected or dynamic single-page portals.
* **Modern Decoupled React 19 SPA:** Fast frontend with TailwindCSS, dark/light themes, instant filtering, and responsive listing cards.
* **Embedded Spring Data JPA & H2 Storage:** High-performance ACID storage with unique B-Tree indexes for O(1) deduplication and full backward-compatible storage abstractions.
* **GraalVM Native Image Ready:** Sub-second startup and minimal memory footprint with Ahead-of-Time (AOT) compilation.

---

## 🌐 Supported Portals

| Portal | Domain | Status | Method / Notes |
| :--- | :--- | :--- : | :--- |
| **Duna House** | [dh.hu](https://dh.hu) | **Active** | Live Jsoup DOM parsing (price, floor area, rooms) |
| **Otthon Centrum** | [oc.hu](https://www.oc.hu) | **Active** | Live Jsoup DOM parsing with Sec-Ch-Ua client hints |
| **Jófogás Ingatlan** | [jofogas.hu](https://ingatlan.jofogas.hu) | **Active** | Live Jsoup DOM search query and regex parsing |
| **Költözzbe** | [koltozzbe.hu](https://koltozzbe.hu) | **Active** | Live DOM and Schema.org JSON-LD parsing |
| **Ingatlanbazár** | [ingatlanbazar.hu](https://ingatlanbazar.hu) | **Active** | Fast internal REST JSON API with cached geo lookup |
| **Ingatlan.com** | [ingatlan.com](https://ingatlan.com) | ⚠️ *Bot-protected* | Graceful fallback link generation (Cloudflare WAF protected) |

---

## 🏗 Architecture & Project Structure

```text
ingatlan-kereso/
├── build.gradle.kts                             # Gradle Kotlin DSL (Java 21, Spring Boot 4.1.0, GraalVM Native)
├── settings.gradle.kts                          # Gradle project settings
├── gradlew / gradlew.bat                        # Gradle Wrapper binaries
├── Dockerfile                                   # Multi-stage GraalVM native container build
├── README.md                                    # Main user and developer documentation
├── gemini.md                                    # Comprehensive system architecture & AI workflow rules
├── data/                                        # Persistent runtime database directory
│   └── ingatlanok_jpa.mv.db                     # Embedded H2 / JPA database
├── frontend/                                    # Modern Decoupled SPA (React 19, TypeScript, Vite, TailwindCSS)
│   ├── src/                                     # React components, Zustand stores, TanStack Query hooks
│   └── package.json                             # Frontend dependencies and test scripts
└── src/main/java/ingatlanok/
    ├── IngatlanApplication.java                 # Spring Boot application entry point (@SpringBootApplication)
    ├── Main.java                                # Hybrid entry point (Spring Boot & CLI mode)
    ├── common/                                  # Cross-cutting utilities (HttpFetcher, PropertyParser, SlugUtils)
    ├── config/                                  # CoreEngineConfig, GraalVmRuntimeHints, SpaWebMvcConfig, GlobalCorsFilter
    ├── core/                                    # Pure business engines (Filter, Deduplication, Analytics, Execution)
    ├── model/                                   # Domain models and value objects
    ├── persistence/                             # Decoupled persistence (ListingStorage, JpaIngatlanStorage, Entities, Repos)
    ├── portal/                                  # Pluggable Portal Engines (DH, OC, Jófogás, Bazár, Költözzbe, IngatlanCom)
    └── web/                                     # Spring MVC REST Controllers, GlobalExceptionHandler, SPA routing
```

---

## 💻 Quickstart

### Prerequisites
* **Java:** JDK 21 LTS or newer
* **Build Tool:** Gradle Wrapper (`gradlew`, included)
* **Node.js:** Node 20+ & npm (required only for building the frontend)

### 1. One-Click Desktop Launch (Windows)
Double-click `START.bat`. The script detects Java and Gradle Wrapper, builds assets if missing, starts the server on port `8080`, and opens the browser.

### 2. Multi-Container Docker Compose Quickstart
Start the full 3-tier architecture (PostgreSQL 17 database, headless Spring Boot backend, and Nginx React 19 frontend reverse proxy) with a single command:

**Windows:**
Double-click `start-docker.bat` or run:
```cmd
start-docker.bat
```

**Linux / macOS:**
Run `start-docker.sh` or run:
```bash
chmod +x start-docker.sh && ./start-docker.sh
```

**Standard Docker Compose Commands:**
```bash
# Pull and start all services in the background
docker compose up -d

# Check service health and running status
docker compose ps

# View logs (e.g. backend)
docker compose logs -f backend

# Gracefully stop all services
docker compose down
```
Once started, access the Web Dashboard at **http://localhost:8080** and the REST API at **http://localhost:8080/api/status**.

### 3. Local Development Build
```bash
# 1. Build Frontend SPA (React 19 + TypeScript + Vite)
cd frontend
npm ci && npm run test && npm run build
cd ..

# 2. Compile, Quality Check and Package Backend (Java 21 + Gradle + Spring Boot)
./gradlew check bootJar

# 3. Run Web Dashboard
java -jar build/libs/ingatlan-kereso-1.0.0.jar

# 4. Optional: Build & Run Native Executable with GraalVM
./gradlew nativeCompile
./build/native/nativeCompile/ingatlan-kereso
```

---

## ⚙️ Configuration & Storage

* **`data/ingatlanok_jpa.mv.db`:** Primary embedded H2 database with unique B-Tree index on `url` for O(1) deduplication.
* **`src/main/resources/application.yml`:** Spring Boot configuration (server port, JPA, Actuator, Virtual Threads).

---

## 🛡️ Developer Workflow & Quality Gate

* **Base Branch:** All feature work branches off from `develop`. Direct pushes to `main` and `develop` are prohibited.
* **Branch Naming:** `feature/<name>`, `fix/<name>`, `docs/<name>`, `refactor/<name>`, `chore/<name>`.
* **Atomic Commits:** Make discrete, descriptive commits using Conventional Commits (`feat: ...`, `fix: ...`). No squash or monolithic merges during development.
* **Worktree Usage:** When developing from an issue, always create a dedicated `git worktree`.
* **Merge Prerequisite:** Pull requests are strictly mergeable **ONLY WHEN ALL CHECKS ARE GREEN** (Java CI, Vitest, runtime smoke test, PR Guard).

---

## 📄 License & Authors

* **Original Author:** Viktor Pusinszky (2017)
* **License:** [GNU General Public License v3.0](LICENSE)
