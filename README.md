# Ingatlan Kereső

[![CI Build](https://img.shields.io/github/actions/workflow/status/Luxeonn/ingatlan-kereso/ci.yml?branch=develop&style=flat-square&label=CI%20Build)](https://github.com/Luxeonn/ingatlan-kereso/actions/workflows/ci.yml)
[![Docker Build](https://img.shields.io/github/actions/workflow/status/Luxeonn/ingatlan-kereso/images.yml?branch=develop&style=flat-square&label=Docker%20Build)](https://github.com/Luxeonn/ingatlan-kereso/actions/workflows/images.yml)
[![Docker Release](https://img.shields.io/github/actions/workflow/status/Luxeonn/ingatlan-kereso/release.yml?style=flat-square&label=Docker%20Release)](https://github.com/Luxeonn/ingatlan-kereso/actions/workflows/release.yml)
[![Release](https://img.shields.io/github/v/release/Luxeonn/ingatlan-kereso?style=flat-square&label=Verzi%C3%B3&color=007ec6)](https://github.com/Luxeonn/ingatlan-kereso/releases)
[![License](https://img.shields.io/badge/Licenc-GPL--3.0-blue?style=flat-square)](LICENSE)

![Java](https://img.shields.io/badge/Java-25%20LTS-ed8b00?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6db33f?style=flat-square)
![React](https://img.shields.io/badge/React-19.0-61dafb?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178c6?style=flat-square)
![Vite](https://img.shields.io/badge/Vite-8.3-646cff?style=flat-square)
![Node.js](https://img.shields.io/badge/Node.js-22%2B-4c1?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=flat-square)

Modern, hibatűrő magyar ingatlankereső, piaci elemző és árfigyelő alkalmazás. Párhuzamosan gyűjti és egységesíti a legnagyobb hazai ingatlanportálok hirdetéseit, kiszűri a duplikációkat és adathibákat, követi az árváltozásokat, valamint valós idejű statisztikai (medián Ft/m²) elemzést nyújt.

---

## Főbb funkciók

- **Multi-portál keresés (Virtual Threads):** Párhuzamos adatgyűjtés 6 portálról: **Duna House**, **Otthon Centrum**, **Jófogás**, **Költözzbe**, **Ingatlanbazár** és **Ingatlan.com** (utóbbi Cloudflare védelem esetén közvetlen keresőlink fallbackkel).
- **Kereszt-portál deduplikáció:** A több ügynökségnél is meghirdetett azonos ingatlanokat egyetlen kártyára vonja össze a legolcsóbb aktív árral, lenyitható részletező és forrás-összehasonlító panellel (`PropertyDetailsDrawer`).
- **Egységes típus-normalizáció:** Automatikus besorolás kanonikus kategóriákba (`LAKAS`, `HAZ`, `TELEK`, `GARAZS`, `IPARI_KERESKEDELMI`, `EGYEB_ISMERETLEN`).
- **Piaci analitika és „Alkalmi vétel” detektálás:** Településenkénti és típusonkénti **medián Ft/m²** számítás (különválasztva a lakóterületet és a telekméretet, minimum 5 mintás küszöbbel). A referencia mediánnál legalább 20%-kal olcsóbb hirdetéseket **Alkalmi vétel** jelvénnyel emeli ki.
- **Adatminőség-motor (0–100 pont):** 10 szabályból álló determinisztikus szűrő, amely kizárja az irreális vagy hibás hirdetéseket (`< 70` pont) a statisztikákból, és 3 szintű pajzzsal (`<QualityScoreShield />`) jelzi a megbízhatóságot.
- **Ártörténet és szerződésvezérelt architektúra:** 3 rétegű adatmodell (`Property` → `SourceListing` → `ListingObservation`), fordítási idejű MapStruct mapperekkel (`JpaEntityMapper`, `PresentationMapper`) és OpenAPI 3.1-ből generált frontend típusokkal (`generated-api.ts`).

---

## Rendszerkomponensek

| Réteg / Komponens | Technológia | Szerepkör |
| :--- | :--- | :--- |
| **Frontend (SPA)** | React 19, TypeScript, Tailwind CSS, Zustand, TanStack Query | Keresőpanel (`SearchPanel`), találati lista (`ListingsPanel`) és piaci elemző nézet (`MarketAnalyticsPanel`). |
| **Backend API** | Java 25 LTS, Spring Boot 4.1.2, Spring MVC, MapStruct, Lombok | Headless JSON REST API (`/api/*`), Virtual Threads alapú párhuzamos kiszolgálás. |
| **Core Engine-ek** | `SearchExecutionEngine`, `DeduplicationEngine`, `DataQualityEngine`, `MarketAnalyticsEngine` | Keresés-koordináció, klaszterező deduplikáció, minőségpontozás és medián árstatisztika. |
| **Portal Scraperek** | Jsoup DOM, JSON-LD, belső REST API + `HttpFetcher` | Portálspecifikus kinyerő motorok Rate Limiter és Circuit Breaker védelemmel. |
| **Perzisztencia** | Spring Data JPA, Hibernate, H2 (dev/CLI) / PostgreSQL 17 (prod) | Ingatlanok, portálhirdetések és idősoros ármegfigyelések tárolása. |

---

## Indítás és futtatás

### 1. Docker Compose (Ajánlott – teljes stack)

Elindítja a **PostgreSQL 17** adatbázist, a **Spring Boot** backendet és az **Nginx** reverse proxy mögött futó **React SPA** frontendet:

```bash
# Indítás előre elkészített GHCR image-ekkel:
docker compose pull backend frontend
docker compose up -d --no-build

# VAGY helyi builddel:
docker compose up -d --build
```

- **Windows gyorsindító:** A `scripts/start-docker.bat` interaktív menüt kínál (helyi build, GHCR `latest`, vagy konkrét verzió).
- **Elérés:** [http://localhost:8080](http://localhost:8080)

### 2. Lokális fejlesztői mód

**Előfeltételek:** JDK 25, Node.js 22+, npm.

**Backend indítása** (a projekt gyökérkönyvtárából, beágyazott H2 adatbázissal):

```bash
# Linux / macOS
./gradlew bootRun

# Windows (PowerShell / CMD)
.\gradlew.bat bootRun
```

**Frontend indítása** (külön terminálban, Vite `/api` proxyval):

```bash
cd frontend
npm ci
npm run dev
```

- **Frontend UI:** [http://localhost:5173](http://localhost:5173)
- **Backend REST API állapot:** [http://localhost:8080/api/status](http://localhost:8080/api/status)

---

## Tesztelés és ellenőrzés

```bash
# Backend (Checkstyle, JUnit 5, JaCoCo >= 80% lefedettség, JAR csomagolás)
./gradlew check bootJar

# Frontend (ESLint, TypeScript, Vitest, Vite build, Playwright E2E)
cd frontend && npm run lint && npm run typecheck && npm run test && npm run build && npm run test:e2e
```

---

## Dokumentáció és hivatkozások

- [Rendszerarchitektúra](docs/architecture.md)
- [REST API Specifikáció (OpenAPI 3.1)](openapi/api-spec.yaml)
- [Biztonsági irányelvek](SECURITY.md)
- [Fejlesztői és AI Agent irányelvek](AGENTS.md)
- [Licenc (GPL-3.0)](LICENSE)
