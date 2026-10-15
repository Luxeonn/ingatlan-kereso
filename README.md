# Ingatlan Kereső (Real Estate Scraper & Monitor)

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.9%2B-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Jsoup](https://img.shields.io/badge/Jsoup-1.23.2-6DB33F?style=for-the-badge)](https://jsoup.org/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)

High-performance real estate monitoring and scraping application built with Java 21 LTS Virtual Threads, SQLite WAL persistence, and a decoupled modern React 19 SPA dashboard.

---

## 📌 Overview

**ingatlan-kereso** automates property search across major Hungarian real estate portals. It aggregates active listings in a single unified view, deduplicates known ads, tracks newly discovered properties, and provides actionable market analytics.

* **Unified Multi-City Search:** Monitor multiple cities or predefined regional clusters (e.g., Budapest & Agglomeration, Debrecen area, University cities).
* **0-Crash Resilience:** Graceful degradation on anti-bot protected or dynamic single-page portals.
* **Modern Decoupled React 19 SPA:** Fast frontend with TailwindCSS, dark/light themes, instant filtering, and responsive listing cards.
* **ACID Embedded SQLite Storage:** Fast queries, WAL mode concurrency, and automatic legacy JSON data migration.

---

## 🌐 Supported Portals

| Portal | Domain | Status | Method / Notes |
| :--- | :--- | :---: | :--- |
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
├── build.gradle.kts                             # Gradle Kotlin DSL configuration (Java 21, Nitrite NoSQL, Jsoup, Gson)
├── settings.gradle.kts                          # Gradle project settings
├── gradlew / gradlew.bat                        # Gradle Wrapper binaries
├── README.md                                    # Main user and developer documentation
├── gemini.md                                    # Comprehensive system architecture & AI workflow rules
├── START.bat / start-docker.bat                 # One-click Windows desktop and Docker launchers
├── data/                                        # Persistent runtime database directory
│   ├── ingatlanok_nosql.db                      # Nitrite DB NoSQL document store (MVStore)
│   └── last_config.json                         # Saved search criteria
├── frontend/                                    # Modern Decoupled SPA (React 19, TypeScript, Vite, TailwindCSS)
│   ├── src/                                     # React components, Zustand stores, TanStack Query hooks
│   └── package.json                             # Frontend dependencies and test scripts
└── src/main/java/ingatlanok/
    ├── Main.java                                # Application entry point (Web server & CLI mode)
    ├── common/                                  # Cross-cutting utilities (HttpFetcher, PropertyParser, SlugUtils)
    ├── core/                                    # Pure business engines (Filter, Deduplication, Analytics, Execution)
    ├── model/                                   # Domain models and value objects
    ├── persistence/                             # Decoupled persistence (NitriteListingStorage, NitriteConfigStorage)
    ├── portal/                                  # Pluggable Portal Engines (DH, OC, Jófogás, Bazár, Költözzbe, IngatlanCom)
    └── web/                                     # Embedded Sun HttpServer (Java 21 Virtual Threads, StaticFileHandler)
```

---

## 💻 Quickstart

### Prerequisites
* **Java:** JDK 21 LTS or newer
* **Build Tool:** Gradle Wrapper (`gradlew`, included)
* **Node.js:** Node 20+ & npm (required only for building the frontend)

### 1. One-Click Desktop Launch (Windows)
Double-click `START.bat`. The script detects Java and Gradle Wrapper, builds assets if missing, starts the server on port `8080`, and opens the browser.

### 2. Docker Quickstart (GHCR)
Run with zero manual configuration using Docker:
```bash
docker run -d -p 8080:8080 --name ingatlan-kereso ghcr.io/luxeonn/ingatlan-kereso:latest
```
Or use Docker Compose:
```bash
docker compose up -d
```

### 3. Local Development Build
```bash
# 1. Build Frontend SPA (React 19 + TypeScript + Vite)
cd frontend
npm ci && npm run test && npm run build
cd ..

# 2. Compile, Quality Check and Package Backend (Java 21 + Gradle)
./gradlew check shadowJar

# 3. Run Web Dashboard
java -jar build/libs/ingatlan-1.0.jar
```

---

## ⚙️ Configuration & Storage

* **`data/ingatlanok.db`:** Primary relational SQLite database with WAL mode and indexes (`idx_listings_city`, `idx_listings_portal`, `idx_listings_discovered_at`).
* **`data/last_config.json`:** Last used search parameters (cities, min/max price, property type, active portals).
* **`data/cities/<city-key>.json`:** Backward-compatible per-city JSON exports and fallbacks.

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
