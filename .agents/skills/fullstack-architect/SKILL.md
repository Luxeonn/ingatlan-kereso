---
name: fullstack-architect
description: Full-stack szoftverarchitekt és termékmérnök specialista. Használd ezt a skillt a Java 21 backend (Spring Boot 4.1.0, Spring Framework 7.0.9, Virtual Threads, Spring MVC headless REST API), a perzisztencia réteg (Spring Data JPA, Hibernate, beágyazott H2 / PostgreSQL 17), valamint a modern autonóm React 19 + TypeScript + Tailwind SPA frontend felülettervezéséhez, UI/UX mikróinterakciókhoz és a 0-SyntaxError UI integritáshoz.
---

# Fullstack Architect Specialist

Ez a szakértői skill felel a **rendszerarchitektúráért**, a **réteghatárok integritásáért**, az **adattárolási és perzisztencia rétegért**, valamint a **modern autonóm React 19 webes felhasználói felületért**.

---

## 1. Purpose & Activation (Cél és Aktiválás)

### Elsődleges Küldetés (Primary Mission)
Biztosítani az alkalmazás rétegzett, laza csatolású architektúrájának fenntarthatóságát, a Java 21 Virtual Threads konkurens modell helyességét, a tiszta headless REST API szerződést, a Spring Data JPA típusbiztos és dialektus-független perzisztenciáját, valamint a React 19 + TypeScript frontend kiváló felületi minőségét (0-SyntaxError, 0-type-leakage).

### Aktiválási Feltételek (Activation Triggers)
A skill aktiválása kötelező minden olyan feladatban, PR-ban vagy kódelemzésben, amely érinti az alábbi területeket:
- **Backend architektúra és rétegek**: `src/main/java/ingatlanok/(core|model|persistence|presentation|web)/`
- **Alkalmazás-belépési pont és konfigurációk**: `src/main/java/ingatlanok/IngatlanApplication.java`, `src/main/java/ingatlanok/Main.java`, `src/main/resources/application.yml`
- **Frontend SPA kliens**: `frontend/**` (`src/`, `package.json`, `vite.config.ts`, `nginx.conf`, `index.html`)
- **Építési és architektúra-szintű függőségek**: `build.gradle.kts`, `settings.gradle.kts`
- **PR Címkék**: `architecture`, `enhancement`, `frontend`, `backend`

---

## 2. Scope & Non-Goals (Hatókör és Nem-célok)

### Közvetlen Hatáskör (In-Scope Responsibilities)
1. **4-Rétegű Unidirekcionális Architektúra**:
   - `web/`: Spring MVC `@RestController` headless végpontok (`/api/search`, `/api/stats`, `/api/portal-statuses`, `/api/config`, `/api/status`), CORS és web konfiguráció (`SpaWebMvcConfig`, `GlobalCorsFilter`).
   - `presentation/`: DTO modellek, KPI aggregáció, aggregált piaci statisztikák.
   - `core/`: Szűrési motor (`PropertyFilterEngine`), O(1) deduplikáció (`DeduplicationEngine`), aggregáció és párhuzamos lekérdezés-koordináció (`SearchExecutionEngine`, `MarketAnalyticsEngine`).
   - `persistence/`: Tárhely absztrakció (`ListingStorage`, `ConfigStorage`), Spring Data JPA entitások (`PropertyEntity`, `PriceHistoryEntity`, `ConfigEntity`), repository-k (`PropertyRepository`), és a `JpaIngatlanStorage` implementáció.
2. **Konkurenciamodell (Java 21 LTS)**:
   - Könnyűsúlyú szálak (`spring.threads.virtual.enabled=true`, `Executors.newVirtualThreadPerTaskExecutor()`). Carrier szálak blokkolásának elkerülése.
3. **Perzisztencia Stratégia (Dual-Mode JPA)**:
   - Beágyazott H2 fájlalapú adattár (`jdbc:h2:file:./data/ingatlanok_jpa;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`) CLI/fejlesztői módban.
   - PostgreSQL 17 konténerizált adatbázis éles Docker környezetben.
   - Teljes függetlenség: közvetlen JDBC hívások és elavult beágyazott vagy NoSQL motorok tiltása.
4. **Modern Autonóm React 19 Frontend**:
   - React 19, TypeScript 5.9, Tailwind CSS v4, Vite 8.3.
   - Kliens állapotkezelés (Zustand) és szerver állapot-szinkronizáció (TanStack Query).
   - Reszponzív layout (360px-től 4K-ig), világos/sötét téma, skeleton loading, online Google Fonts CDN (Plus Jakarta Sans).

### Kifejezett Nem-célok (Explicit Non-Goals)
- **Web scraping és portál-specifikus DOM elemzés**: A portálmotorok belső logikája, a Jsoup lekérdezések és a Cloudflare WAF elkerülés a `scraper-security-engineer` hatásköre.
- **CI/CD munkafolyamatok, konténer hardening és JaCoCo kalkuláció**: A GitHub Actions (`ci.yml`, `pr-review.yml`), a multi-stage Dockerfile optimalizálás és a tesztlefedettségi kapuk a `quality-devops-engineer` hatásköre.

---

## 3. Required Workflow (Kötelező Munkafolyamat)

A specialistának minden felülvizsgálat során a következő 4 fázisú folyamatot kell végrehajtania:

```
┌─────────────────────────────────┐
│ 1. Kontextusfeltárás & Audit   │ ➔ Fájllista, réteg-besorolás, módosítási cél
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 2. Architektúra-ellenőrzés      │ ➔ Réteghatárok, Storage Abstraction, Virtual Threads
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 3. Építési & Típusellenőrzés    │ ➔ ./gradlew compileJava, npm run typecheck
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 4. Szintézis & Döntéshozatal    │ ➔ Thematic Gate státusz (NO_FINDING / FINDING)
└─────────────────────────────────┘
```

1. **1. fázis: Kontextusfeltárás és Fájlaudit**:
   - Határozd meg az érintett fájlok listáját.
   - Ellenőrizd, hogy a módosítás réteghatárokat (pl. Web -> Core -> Persistence) vagy a Frontend/Backend REST szerződést érinti-e.
2. **2. fázis: Architektúra- és Szabályellenőrzés**:
   - Vizsgáld meg, hogy nincs-e réteginverzió (pl. a `core` csomag nem hivatkozik-e `web` vagy `presentation` osztályokra).
   - Ellenőrizd, hogy az adatbázis-műveletek kizárólag a `ListingStorage` és `ConfigStorage` interfészeken keresztül valósulnak-e meg.
   - Vizsgáld meg, hogy a Virtual Threads környezetben nincs-e szinkronizált blokk (`synchronized`) hosszú I/O műveletek felett.
3. **3. fázis: Empirikus és Építési Ellenőrzés**:
   - Backend fordítás ellenőrzése: `./gradlew compileJava`.
   - Frontend típusintegritás ellenőrzése: `cd frontend && npm run typecheck` (vagy `npx tsc --noEmit`).
   - Ellenőrizd, hogy nem keletkezett-e lint vagy szintaktikai hiba.
4. **4. fázis: Szintézis és Döntéshozatal**:
   - Döntési token meghatározása (`NO_FINDING`, `FINDING`, `NOT_APPLICABLE`, `NOT_VERIFIED`).
   - Pontosan 1 mondatos szakmai indoklás megfogalmazása a PR Gate számára.

---

## 4. Review Checklist (Felülvizsgálati Ellenőrzőlista)

### A. Rendszerarchitektúra és Réteghatárok
- [ ] A 4 réteg közötti függőségi irány szigorúan felülről lefelé halad: `web` -> `presentation` -> `core` -> `persistence` / `portal`.
- [ ] Tilos a réteginverzió: alsóbb réteg nem importálhat felsőbb rétegbeli osztályt.
- [ ] A Spring Boot alkalmazás 100%-ban headless REST API (`spring.web.resources.add-mappings: false`), nem szolgál ki beágyazott statikus HTML-t a backend jar-ból.
- [ ] A REST végpontok URL formátuma és JSON DTO szerződése konzisztens a frontend klienssel.

### B. Virtual Threads és Konkurenciakezelés
- [ ] A virtuális szálak engedélyezettek (`spring.threads.virtual.enabled=true`).
- [ ] Nincsenek carrier szálat blokkoló hívások (`synchronized` blokkok I/O felett, `ThreadLocal` memóriaszivárgások).
- [ ] Párhuzamos scrapinghez és aggregációhoz `Executors.newVirtualThreadPerTaskExecutor()` vagy Spring aszinkron mechanizmus használt.

### C. Perzisztencia és Spring Data JPA
- [ ] A perzisztens réteg kizárólag Spring Data JPA entitásokat és repository-kat használ.
- [ ] Minden perzisztencia-hozzáférés a `ListingStorage` vagy `ConfigStorage` interfészen keresztül történik.
- [ ] Zéró közvetlen JDBC/SQL hívás az üzleti logikában.
- [ ] Nincsenek elavult perzisztencia-motorok vagy nem támogatott adatbázis-meghajtók a kódban.
- [ ] Az entitások (PropertyEntity, PriceHistoryEntity) indexelése biztosítja az O(1) deduplikációt és gyors lekérdezést.

### D. Frontend Integritás és UI/UX
- [ ] A frontend kód típusbiztos (`tsc --noEmit` 0 hibával fut le).
- [ ] A komponensek React 19 szabványos hookokat használnak, nincsenek memory leak-ek vagy felesleges re-renderek.
- [ ] A szerverállapot kezelése TanStack Query-vel történik, megfelelő hibakezelési és loading skeleton állapotokkal.
- [ ] A felület teljesen reszponzív mobil (360px) és asztali (4K) nézetekben egyaránt.

---

## 5. Evidence & Output (Bizonyítékok és Kimenet)

A felülvizsgálat során a szakértő köteles az alábbi kézzelfogható bizonyítékokat rögzíteni:
- **Architekturális ellenőrzési lelet**: Érintett interfészek és osztályok listája pontos fájl- és sorhivatkozással.
- **Fordítási és típusvizsgálati kimenet**: `./gradlew compileJava` és `npm run typecheck` eredménye.
- **REST Szerződés-ellenőrzés**: Változatlan vagy frissített DTO struktúrák dokumentálása.

### Jóváhagyási Formátum (Thematic Agent Approval Gate Output)
A Pull Request leírásának Thematic Gate blokkjába illesztendő szöveg:
```markdown
- [x] Fullstack Architecture & UI (`fullstack-architect`): [NO_FINDING / NOT_APPLICABLE] – [Pontosan 1 tömör összefoglaló mondat az architekturális integritásról és UI típusbiztonságról]
```
*Megjegyzés: `FINDING` státusz esetén az `[x]` helyett `[ ]` marad, a státusz `[FINDING]`, és a PR leírásában vagy kommentben részletes hibajelentést kell csatolni.*

---

## 6. Standard Decision Semantics (Döntési Szemantika)

- **`FINDING`**: Architekturális réteginverzió, carrier szál blokkolása, közvetlen adatbázis-kapcsolódás a core-ból, nem támogatott perzisztencia technológia használata, vagy TypeScript fordítási hiba esetén.
  - *Kötelező formátum*: `FINDING: [Súlyosság: CRITICAL/MAJOR/MINOR] - [Fájl:Sor] - [Leírás és javítási útmutató]`
- **`NO_FINDING`**: Az architektúra, perzisztencia és frontend réteg maradéktalanul megfelel a követelményeknek, 0 hiba és 0 figyelmeztetés mellett.
- **`NOT_APPLICABLE`**: A feladat kizárólag scraper motorokat vagy CI/CD konfigurációkat érint, és nem változtatja meg a backend architektúrát, REST API-t, adatmodellt vagy a frontendet.
- **`NOT_VERIFIED`**: A fordítás vagy típusellenőrzés nem futtatható le (pl. hiányzó node/npm vagy gradle környezet miatt). Fail-closed védelemként a PR nem hagyható jóvá!

---

## 7. Stop Conditions (Megállási Feltételek)

A szakértő azonnal MEGÁLL, elutasítja a jóváhagyást, és `FINDING` státuszt állít ki az alábbi feltételek bármelyike esetén:
1. **Réteghatár-sértés (Layer Inversion)**: A `core`, `persistence` vagy `portal` réteg közvetlenül hivatkozik a `web` vagy `presentation` osztályaira.
2. **Virtual Threads Blokkolás**: Szinkronizált blokk (`synchronized`) vagy blokkoló hívás hosszú futású I/O művelet köré vonva.
3. **Elavult Adatbázis Visszavezetése**: Nem jóváhagyott adatbázis-meghajtók vagy elavult beágyazott tárolási könyvtárak megjelenése a függőségek vagy a forráskód között.
4. **Storage Abstraction Megkerülése**: Közvetlen SQL vagy DataSource hívás a Web Controllerből vagy a Core modulokból.
5. **Frontend Típus- vagy Fordítási Hiba**: A `tsc --noEmit` hibát jelez, vagy szintaktikai hiba van a React komponensekben.
6. **Statikus Fájlkiszolgálás a Backendben**: HTML/JS/CSS csomagolása a Spring Boot JAR-ba ahelyett, hogy a független frontend konténer szolgálná ki.

---

## 8. Completion Criteria (Befejezési Kritériumok)

A `fullstack-architect` jóváhagyása (`NO_FINDING`) kizárólag akkor adható meg, ha az alábbi kritériumok MINDENGYIKE teljesül:
1. A backend forráskód hibátlanul lefordul: `./gradlew compileJava` zöld.
2. A frontend forráskód típusellenőrzése sikeres: `cd frontend && npm run typecheck` 0 hibával lefut.
3. A rétegzett unidirekcionális függőségek tiszták, réteginverzió nincs.
4. A Spring Data JPA entitások és repository-k megfelelnek a specifikációnak.
5. A REST API végpontok szerződése és DTO struktúrája összhangban van a frontend klienssel.
6. A PR Gate blokkban pontosan egy mondatos, szakmai, nem sablon indoklás szerepel.
