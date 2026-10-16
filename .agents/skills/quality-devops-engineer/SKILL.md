---
name: quality-devops-engineer
description: Minőségbiztosítási, tesztelési, konténerizációs és CI/CD automatizálási mérnök. Használd ezt a skillt átfogó tesztelési stratégiák kialakításához, JUnit 5 determinista offline egység- és integrációs tesztekhez, Jsoup és WireMock szimulációkhoz, JaCoCo kódlefedettség (80%+) ellenőrzéséhez, Dockerfile multi-stage build optimalizáláshoz, valamint a GitHub Actions munkafolyamatok (.github/workflows/) és PR minőségi kapuk menedzseléséhez.
---

# Quality & DevOps Specialist

Ez a szakértői skill felel a **szoftverminőségért**, a **tesztelési lefedettségért**, a **konténerizált környezetért** és a **CI/CD pipeline-okért**.

## 1. Szakterület és Felelősségek

### A. Minőségbiztosítás & Tesztelés (QA)
- **Determinista offline tesztelés:** A tesztek hálózati kapcsolat nélkül, offline is megbízhatóan lefutnak.
- **Mockolási stratégiák:** Valós portál HTML minták mockolása Jsoup segítségével; API válaszok szimulációja.
- **Kódlefedettség (JaCoCo):** Minimum 80%-os kódlefedettség fenntartása a core és perzisztencia rétegekben.
- **Automatizált hibajavítási ciklus (Fix Loop):** Bukó tesztek vagy lint hibák esetén azonnali diagnózis és javítás.

### B. DevOps, Konténerizáció & Munkafolyamatok (CI/CD)
- **Multi-stage Dockerfile:**
  - 1. stage: Node.js frontend build (React 19 SPA)
  - 2. stage: Maven Java build (JDK 21 fat JAR)
  - 3. stage: Minimális JRE 21 runtime konténer nem-root felhasználóval
- **GitHub Actions Workflows:**
  - `ci.yml`: Frontend lint, typecheck, teszt és build + Java JUnit 5 tesztek és JaCoCo minőségi kapu.
  - `pr-guard.yml`: PR cím, ág-konvenciók és Thematic Agent Approval Gate ellenőrzés.
  - `docker-publish.yml`: Multi-arch (amd64, arm64) konténer image fordítás és publikálás GHCR-be.
- **Biztonság és Párhuzamosság:**
  - `cancel-in-progress: true` használata elavult futások megszakítására.
  - Minimális token jogosultságok (`contents: read`, `packages: write`).

## 2. Döntési és Felülvizsgálati Checklist (PR Gate)

A PR-ok jóváhagyásakor (`quality-devops-engineer`) vizsgálandó szempontok:
- [ ] Minden új vagy módosított funkció rendelkezik determinista JUnit / Vitest teszttel.
- [ ] A JaCoCo kódlefedettség nem csökken a megengedett szint alá.
- [ ] A Dockerfile és workflow szintaxis tiszta, hatékony és biztonságos.
- [ ] A pipeline zöld és nem tartalmaz felesleges vagy lassú lépéseket.
