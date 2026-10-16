---
name: fullstack-architect
description: Full-stack szoftverarchitekt és termékmérnök specialista. Használd ezt a skillt a Java 21 backend (Virtual Threads, 4-rétegű architektúra, REST API), a beágyazott Nitrite DB NoSQL perzisztencia (MVStore, B-Tree indexelés, O(1) deduplikáció), valamint a modern React 19 + TypeScript + Tailwind SPA frontend felülettervezéséhez, UI/UX mikróinterakciókhoz és a 0-SyntaxError UI integritáshoz.
---

# Fullstack Architect Specialist

Ez a szakértői skill egyesíti a **rendszerarchitektúrát**, az **adattárolási réteget** és a **modern webes felhasználói felületet**.

## 1. Szakterület és Felelősségek

### A. Rendszerarchitektúra & Java 21 Backend
- **Java 21 LTS & Virtual Threads:** Könnyűsúlyú szálak használata (`Executors.newVirtualThreadPerTaskExecutor()`). A carrier szálak blokkolása tilos.
- **4-Rétegű Szeparáció:**
  1. `web/` és `presentation/`: Beágyazott HTTP szerver, REST endpointok, DTO-k.
  2. `core/`: Szűrési logika, deduplikáció, párhuzamos aggregáció, piaci analitika.
  3. `portal/`: Nyílt/Zárt elvű (OCP) pluggable portálmotorok.
  4. `persistence/`: NoSQL dokumentumtár és konfigurációkezelés.
- **0-Crash Elv:** Külső hiba, hálózati kimaradás vagy DOM-változás soha nem állíthatja le a szervert.

### B. NoSQL Perzisztencia & Adatmodell
- **Nitrite DB (MVStore backend):** `ListingStorage` és `ConfigStorage` interfészek mögé rejtett dokumentum-alapú tárolás.
- **O(1) Deduplikáció:** B-Tree indexelt kulcsok mentén gyors meglévő/új állapot azonosítás.
- **Konkurenciakezelés:** Szálbiztos írás és olvasás, atomi állapotmegőrzés a `data/ingatlanok_nosql.db` fájlban.

### C. Modern React 19 SPA & UI Integritás
- **Frontend Stack:** React 19, TypeScript, Tailwind CSS, Vite (`frontend/`).
- **Fat JAR Integráció:** A fordított build (`frontend/dist/`) beágyazása a `src/main/resources/static/` könyvtárba.
- **UI/UX & Reszponzivitás:** Mobilbarát (360px-től 4K-ig), világos/sötét téma, KPI kártyák, szűrők, skeleton loading.
- **0-SyntaxError Integritás:** Szigorú TypeScript típusellenőrzés (`npm run typecheck`), ESLint és Vitest tesztek.

## 2. Döntési és Felülvizsgálati Checklist (PR Gate)

A PR-ok jóváhagyásakor (`fullstack-architect`) vizsgálandó szempontok:
- [ ] Tiszta réteghatárok és laza csatolás a Java osztályok között.
- [ ] Nem blokkoló Virtual Threads I/O és robusztus kivételkezelés.
- [ ] NoSQL dokumentumstruktúra és indexek séma-konzisztenciája.
- [ ] Hibátlan TypeScript fordítás és zéró runtime hiba a felületen.
- [ ] A beágyazott SPA helyesen kommunikál a backend REST végpontjaival.
