---
name: fullstack-architect
description: Full-stack szoftverarchitekt és termékmérnök specialista. Használd ezt a skillt a Java 21 backend (Spring Boot 4.1.0, Spring Framework 7.0.8, GraalVM Native Image AOT, Virtual Threads, Spring MVC REST API), a perzisztencia réteg (Spring Data JPA, Hibernate, beágyazott H2 / NoSQL kompatibilitási interfészek), valamint a modern React 19 + TypeScript + Tailwind SPA frontend felülettervezéséhez, UI/UX mikróinterakciókhoz és a 0-SyntaxError UI integritáshoz.
---

# Fullstack Architect Specialist

Ez a szakértői skill egyesíti a **rendszerarchitektúrát**, az **adattárolási réteget** és a **modern webes felhasználói felületet**.

## 1. Szakterület és Felelősségek

### A. Rendszerarchitektúra & Java 21 Spring Boot Backend
- **Spring Boot 4.1.0 & Spring Framework 7.0.8:** Modern IoC/DI, deklaratív konfigurációk, AOT (Ahead-of-Time) és GraalVM Native Image támogatás (`native-maven-plugin` / `native-gradle-plugin`).
- **Java 21 LTS & Virtual Threads:** Könnyűsúlyú szálak használata webkiszolgáláshoz (`spring.threads.virtual.enabled=true`) és párhuzamos feladatvégrehajtáshoz (`Executors.newVirtualThreadPerTaskExecutor()`). A carrier szálak blokkolása tilos.
- **Rétegzett Szeparáció:**
  1. `web/`: Spring MVC `@RestController` végpontok, `SpaWebMvcConfig` (SPA routing és statikus asset kiszolgálás), `GlobalCorsFilter`.
  2. `presentation/`: DTO modellek, KPI aggregáció, portál státuszkártyák.
  3. `core/`: Szűrési motor, deduplikáció, párhuzamos aggregáció, piaci analitika.
  4. `portal/`: Nyílt/Zárt elvű (OCP) pluggable portálmotorok.
  5. `persistence/`: Spring Data JPA entitások (`entity/`), interfészek (`repository/`), és a `JpaIngatlanStorage` implementáció (`ListingStorage`, `ConfigStorage` interfészekkel), valamint a legacy NoSQL Nitrite DB támogatás.
- **0-Crash Elv:** Külső hiba, hálózati kimaradás vagy portál API/DOM változás soha nem állíthatja le a szervert.

### B. Perzisztencia & Adatmodell
- **Spring Data JPA & Hibernate:** Deklaratív repository-k, beágyazott és perzisztens H2 fájlalapú adattár (`jdbc:h2:file:./data/ingatlanok_jpa;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`), tranzakciókezelés.
- **Laza csatolás (Storage Abstraction):** A core réteg a `ListingStorage` és `ConfigStorage` interfészeken keresztül kommunikál az adattárral, így a JPA és NoSQL motorok transzparensen cserélhetők.
- **O(1) Deduplikáció és Indexelés:** Indexelt kulcsok (ID, URL) mentén gyors meglévő/új állapot azonosítás és ár-előzmény naplózás.

### C. Modern React 19 SPA & UI Integritás
- **Frontend Stack:** React 19, TypeScript, Tailwind CSS, Vite (`frontend/`).
- **BootJar & Native Image Integráció:** A fordított frontend SPA (`frontend/dist/`) beágyazása a `src/main/resources/static/` könyvtárba, melyet a Spring MVC transzparensen szolgál ki SPA fallback útválasztással.
- **UI/UX & Reszponzivitás:** Mobilbarát (360px-től 4K-ig), világos/sötét téma, KPI kártyák, szűrők, skeleton loading.
- **0-SyntaxError Integritás:** Szigorú TypeScript típusellenőrzés (`npm run typecheck`), ESLint és Vitest tesztek.

## 2. Döntési és Felülvizsgálati Checklist (PR Gate)

A PR-ok jóváhagyásakor (`fullstack-architect`) vizsgálandó szempontok:
- [ ] Tiszta réteghatárok és laza csatolás a Spring Boot komponensek között.
- [ ] GraalVM Native Image kompatibilitás (AOT-barát reflexió, Spring Boot 4 konvenciók).
- [ ] Nem blokkoló Virtual Threads I/O és robusztus kivételkezelés.
- [ ] JPA entitások, migrációk és repository-k helyes működése és perzisztenciája.
- [ ] Hibátlan TypeScript fordítás és zéró runtime hiba a felületen.
- [ ] A beágyazott SPA helyesen kommunikál a Spring Boot REST végpontjaival.
