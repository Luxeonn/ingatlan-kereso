---
name: quality-devops-engineer
description: Minőségbiztosítási, tesztelési, konténerizációs és CI/CD automatizálási mérnök. Használd ezt a skillt átfogó tesztelési stratégiák kialakításához, JUnit 5 determinista offline egység- és integrációs tesztekhez, MockMvc és WireMock szimulációkhoz, JaCoCo kódlefedettség (80%+) ellenőrzéséhez, GraalVM Native Image multi-stage Dockerfile optimalizáláshoz, valamint a GitHub Actions munkafolyamatok (.github/workflows/) és PR minőségi kapuk menedzseléséhez.
---

# Quality & DevOps Specialist

Ez a szakértői skill felel a **szoftverminőségért**, a **tesztelési lefedettségért**, a **konténerizált környezetért** és a **CI/CD pipeline-okért**.

## 1. Szakterület és Felelősségek

### A. Minőségbiztosítás & Tesztelés (QA)
- **Determinista offline tesztelés:** A tesztek hálózati kapcsolat nélkül, offline is megbízhatóan lefutnak.
- **Spring Boot tesztelés:** `@SpringBootTest`, `@AutoConfigureMockMvc` és `MockMvc` használata a REST végpontok szerződés- és hibatesztjeihez.
- **Mockolási stratégiák:** Valós portál HTML minták mockolása Jsoup segítségével; API válaszok és tárolók szimulációja.
- **Kódlefedettség (JaCoCo):** Minimum 80%-os kódlefedettség fenntartása a core és perzisztencia rétegekben (`jacocoTestReport`, `jacocoTestCoverageVerification`).
- **Statikus analízis:** Checkstyle és SpotBugs szabályok betartása; AOT által generált források kizárása a hibás riasztások elkerülésére.
- **Automatizált hibajavítási ciklus (Fix Loop):** Bukó tesztek vagy lint hibák esetén azonnali diagnózis és javítás.

### B. DevOps, Konténerizáció & Munkafolyamatok (CI/CD)
- **Multi-stage Dockerfile (GraalVM Native Image):**
  - 1. stage: Node.js frontend build (React 19 SPA Vite)
  - 2. stage: GraalVM native image builder (`ghcr.io/graalvm/native-image-community:21`) vagy standard JDK 21 build
  - 3. stage: Minimális Debian/Distroless runtime konténer nem-root felhasználóval
- **GitHub Actions Workflows:**
  - `ci.yml`: Párhuzamosított `frontend` és `backend` jobok komponens alapú útvonalszűréssel (`dorny/paths-filter`), Gradle `bootJar` és headless smoke teszttel.
  - `pr-review.yml`: PR cím, ág-konvenciók és Thematic Agent Approval Gate (3 pillér) ellenőrzés.
  - `docker-publish.yml`: Multi-arch konténer image fordítás és publikálás GHCR-be.
- **Biztonság és Párhuzamosság:**
  - `cancel-in-progress: true` használata elavult futások megszakítására.
  - Minimális token jogosultságok (`contents: read`, `pull-requests: read`, `packages: write`).

## 2. Döntési és Felülvizsgálati Checklist (PR Gate)

A PR-ok jóváhagyásakor (`quality-devops-engineer`) vizsgálandó szempontok:
- [ ] Minden új vagy módosított funkció rendelkezik determinista JUnit / MockMvc / Vitest teszttel.
- [ ] A JaCoCo kódlefedettség nem csökken a megengedett szint alá.
- [ ] A Checkstyle és SpotBugs elemzések 0 hibával futnak le.
- [ ] A Dockerfile és workflow szintaxis tiszta, hatékony, párhuzamosított és biztonságos.
- [ ] A pipeline zöld és nem tartalmaz felesleges vagy blokkoló lépéseket.
