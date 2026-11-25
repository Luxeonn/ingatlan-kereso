# 🏡 Ingatlan Kereső

[![CI Build](https://img.shields.io/github/actions/workflow/status/Luxeonn/ingatlan-kereso/ci.yml?branch=develop&style=flat-square&label=CI%20Build)](https://github.com/Luxeonn/ingatlan-kereso/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Luxeonn/ingatlan-kereso?style=flat-square&label=Verzi%C3%B3&color=007ec6)](https://github.com/Luxeonn/ingatlan-kereso/releases)
[![License](https://img.shields.io/badge/Licenc-GPL--3.0-blue?style=flat-square)](LICENSE)

![Java](https://img.shields.io/badge/Java-25%20LTS-ed8b00?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6db33f?style=flat-square)
![React](https://img.shields.io/badge/React-19.0-61dafb?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178c6?style=flat-square)
![Vite](https://img.shields.io/badge/Vite-8.3-646cff?style=flat-square)
![Node.js](https://img.shields.io/badge/Node.js-22%2B-4c1?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=flat-square)

<br />

> **Láss át a hazai ingatlanpiaci zajon.**  
> Egyetlen felület, amely helyetted fésüli át a legnagyobb magyar ingatlanportálokat, kiszűri a trükkös duplikációkat, leleplezi a rejtett árcsökkenéseket, és megmutatja, hol vannak a valódi piaci lehetőségek.

<br />

---

<br />

## ✨ Miért más az Ingatlan Kereső?

A hagyományos portálokon ugyanazt az ingatlant tucatnyi ügynökség hirdeti eltérő áron, pontatlan adatokkal és mesterségesen felpumpált kínálattal. Az **Ingatlan Kereső** a háttérben elvégzi a szűrést és elemzést:

- 🚀 **Párhuzamos portál-aggregáció 6 hazai portálról**  
  Egyetlen kereséssel gyűjti össze a kínálatot (`dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, `ingatlan.com`) virtuális szálakon futó, hibatűrő motorral.

- 🧩 **Intelligens duplikáció-szűrés és kézi forráskezelés (Split / Merge)**  
  Összevonja az azonos ingatlanok többportálos hirdetéseit a **legkedvezőbb aktív árral**, és a `DedupController` API-n keresztül lehetővé teszi a téves összevonás leválasztását („Nem ugyanaz”), több kártya kézi összevonását (`Ctrl+kattintás` + „Összevonás”), valamint a felülbírálások visszavonását.

- 🛡️ **Ingatlan.com Sidecar és böngészős HTML-import**  
  A Cloudflare-védett portált on-demand Node.js Playwright Chromium sidecarral kérdezi le; korlátozás esetén közvetlen keresőlinket és böngészős HTML-importot biztosít a további oldalakhoz.

- 💎 **Szegmentált piaci radar és „Alkalmi vétel” vadász**  
  Külön számítja a lakóingatlan- és telekpiaci medián m² árat, legalább 20%-os árelőnynél „Alkalmi vétel” jelölést ad, és interaktív grafikonokon mutatja az árszórást, városi összevetést és áresési toplistát.

- 🔍 **Beépített adatminőség-védelem, ártörténet és mentett presetek**  
  0–100 pontos adatminőségi pajzzsal szűri a kiugró vagy hiányos hirdetéseket, naplózza az árváltozásokat és megtekintési állapotokat, továbbá támogatja az egykattintásos mentett keresési sablonokat (presetek), kedvenceket és archívumot.

<br />

---

<br />

## 🏗️ Milyen komponensekből épül fel?

| Komponens | Technológiai alap | Mit csinál? |
| :--- | :--- | :--- |
| 🎨 **Webes Felület (SPA)** | React 19, TypeScript 5.9, Vite 8.3, Tailwind CSS 4 | Letisztult keresőfelület, szűrők, kézi dedup vezérlők és piaci elemző dashboard (élesben Nginx proxy mögött). |
| ⚡ **Headless REST API** | Java 25 LTS, Spring Boot 4.1.2 | Virtuális szálakra épülő backend API (`RestApiController`, `DedupController`), OpenAPI szerződéssel. |
| 🧠 **Üzleti és Elemző Motor** | Saját domain algoritmusok | Normalizálás, klaszteres duplikáció-kezelés, 0–100 adatminőség és szegmentált piaci statisztikák. |
| 🕷️ **Portál Konnektorok & Sidecar** | Jsoup, REST kliens, Node.js / Playwright | Párhuzamos portál-adatgyűjtés SSRF-védett `HttpFetcher`-en keresztül, automatikus sidecar fallbackkel. |
| 🗄️ **Perzisztencia** | Spring Data JPA, H2 / PostgreSQL 17 | Ingatlanok, megfigyelések, ártörténet, kézi dedup felülbírálások és keresési presetek tárolása. |

<br />

---

<br />

## 🚀 Hogyan indítsd el?

### 🐳 1. Indítás Dockerrel (ajánlott)

Egyetlen lépésben elindítja a teljes rendszert (PostgreSQL 17, backend + beépített sidecar futtatókörnyezetek és Nginx frontend):

```bash
# Előre felépített képek letöltése és indítása:
docker compose pull backend frontend
docker compose up -d --no-build

# VAGY teljes felépítés helyi forráskódból:
docker compose up -d --build
```

> 💡 **Windows tipp:** A `scripts/start-docker.bat` interaktív menüből indítja a lokális buildet vagy a GHCR képeket.

👉 Indítás után nyisd meg: **[http://localhost:8080](http://localhost:8080)**

### 💻 2. Lokális fejlesztői indítás

Előfeltételek: **JDK 25** és **Node.js 22+** (opcionálisan `playwright-core` + Chromium az Ingatlan.com sidecarhoz, lásd [tools/README.md](tools/README.md)).

**1. Backend indítása:**

```bash
# Linux / macOS
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```

**2. Frontend indítása** *(külön terminálban)*:

```bash
cd frontend
npm ci
npm run dev
```

👉 Fejlesztői felület: **[http://localhost:5173](http://localhost:5173)** *(automatikusan a `localhost:8080` API-hoz csatlakozik)*.

<br />

---

<br />

## 🧪 Minőségellenőrzés

```bash
# Gyors, összevont ellenőrzés (backend + frontend)
bash scripts/ai-verify.sh --all

# Teljes backend kapu (Checkstyle, ArchUnit, JUnit 5, JaCoCo >= 80%, bootJar)
./gradlew checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification check bootJar

# Teljes frontend kapu (OpenAPI típusgenerálás, TypeScript, ESLint, Vitest, Vite build, E2E)
cd frontend && npm run generate-api && npm run typecheck && npm run lint && npm run test && npm run build
npm run test:e2e

# Dokumentációs és architekturális guardrail ellenőrzés (7/7)
bash scripts/validate-skills.sh -v
```

<br />

---

<br />

## 📚 További tudnivalók

- 🤝 [Közreműködői útmutató](CONTRIBUTING.md) *(branch/worktree, PR-megfelelőség, CI/CD)*
- ♿ [Akadálymentességi útmutató (WCAG 2.2 AA)](Accessibility.md)
- 🔒 [Biztonsági irányelvek (AppSec & hibajelentés)](SECURITY.md)
- 🎨 [Frontend SPA útmutató](frontend/README.md)
- 🎭 [Scraper Sidecar (Node.js & Playwright)](tools/README.md)
- 🏛️ [Rendszerarchitektúra részletesen](docs/architecture.md)
- 🔌 [REST API Specifikáció (OpenAPI)](openapi/api-spec.yaml)
- ⚖️ [Licenc (GPL-3.0)](LICENSE)
