# Ingatlan Kereső - Rendszer-architektúra és Fejlesztési Irányelvek (GEMINI.md)

Ez a dokumentum a projekt mélyreható technikai és rendszer-architektúráját, a csomag- és komponensfelelősségeket, a web scraping mechanizmusokat, valamint az AI asszisztensek és belső fejlesztők számára előírt kötelező verziókezelési és közreműködési szabályzatot rögzíti.

---

## 1. Rendszerarchitektúra és Működési Folyamat

Az **ingatlan-kereso** egy Java 21 LTS alapú asztali alkalmazás, amely egy beágyazott, virtuális szálakkal (`Virtual Threads`) működő HTTP REST webszerverből, egy reszponzív egyoldalas webes felhasználói felületből (SPA), egy keresést koordináló orchestrator rétegből, valamint polimorf web scraper modulokból áll.

### Működési Folyamat (System Workflow)

```mermaid
flowchart TD
    subgraph UI ["Frontend Réteg (Browser SPA)"]
        UI_View["WebDashboardHtml: Dinamikus UI"]
        UI_Event["Keresési indítás / Szűrő módosítás"]
        UI_View --> UI_Event
    end

    subgraph Server ["Szerver Réteg (Java 21 WebServer)"]
        HTTP_EP["Sun HttpServer (port 8080)"]
        V_Thread["Executors.newVirtualThreadPerTaskExecutor()"]
        HTTP_EP --> V_Thread
    end

    subgraph Service ["Szolgáltatási és Scraper Réteg"]
        Orchestrator["IngatlanSearchService: Koordináció"]
        P1["DunaHouseScraper: dh.hu"]
        P2["OtthonCentrumScraper: oc.hu"]
        P3["JofogasScraper: jofogas.hu"]
        P4["IngatlanComScraper: fallback"]
        P5["IngatlanBazarScraper: fallback"]
        
        Orchestrator --> P1 & P2 & P3 & P4 & P5
    end

    subgraph Persistence ["Perzisztencia Réteg (JSON Tároló)"]
        Repo["IngatlanRepository (Singleton)"]
        CityDB[("data/cities/<cityKey>.json")]
        GlobalDB[("data/ingatlanok_db.json")]
        Config[("data/last_config.json")]
        
        Repo --> CityDB
        Repo --> GlobalDB
        Repo --> Config
    end

    UI_Event -->|POST /api/search| HTTP_EP
    V_Thread --> Orchestrator
    P1 & P2 & P3 & P4 & P5 -->|IngatlanItem lista| Repo
    Repo -->|Duplikátum szűrés & Új jelölés| Orchestrator
    Orchestrator -->|SearchResult JSON| HTTP_EP
    HTTP_EP -->|JSON Válasz| UI_View
```

### Keresési és Adatkezelési Lépések

1. **Konfiguráció betöltése:** Induláskor az `IngatlanRepository` betölti a legutóbbi keresési paramétereket a `data/last_config.json` fájlból, valamint memóriába olvassa a városonkénti előzményeket a `data/cities/*.json` fájlokból.
2. **Keresési kérés fogadása:** A felhasználó a webes felületen vagy a CLI-n keresztül keresést indít. A `WebServer` virtuális szálon továbbítja a `SearchCriteria` objektumot az `IngatlanSearchService` felé.
3. **Párhuzamos scraping:** Az orchestrator végighívja az aktív portálok scraper implementációit (`IngatlanScraper` interfész). A scraperek a közös `HttpFetcher` és `PropertyParser` modulok segítségével letöltik a HTML tartalmat, feldolgozzák a hirdetéskártyákat, és kinyerik az árat, alapterületet, szobaszámot és a közvetlen linket.
4. **Duplikátumszűrés és frissesség megállapítása:** Az `IngatlanRepository` ellenőrzi, hogy a talált URL szerepel-e már az adott város vagy a globális adatbázis ismert linkjei között.
   - Ha a link korábban nem volt ismert, megkapja a `✨ ÚJ TALÁLAT` jelölést (`newItem = true`), és rögzítésre kerül az adatbázisban a felfedezés időpontjával.
5. **Kliens értesítés és asztali böngészés:** Az aggregált `SearchResult` JSON válaszként visszakerül a frontendhez. Amennyiben a felhasználó engedélyezte a lapmegnyitást, a `BrowserUtils.openUrls()` megnyitja a friss linkeket az operációs rendszer alapértelmezett böngészőjében (biztonsági limitekkel védve a tab-túlcsordulás ellen).

---

## 2. Csomagstruktúra és Komponens Felelősségek

```text
ingatlan-kereso/
├── src/main/java/
│   └── ingatlanok/
│       ├── Main.java                            # Belépési pont (Web Dashboard szerver és CLI mód vezérlő)
│       ├── common/                              # Közös megosztott modul (lapos hierarchia)
│       │   ├── BrowserUtils.java                # Asztali böngészőkezelés (Desktop API / process fallback), limitekkel
│       │   ├── CityUtils.java                   # Településnevek normalizálása, előre definiált városlista, presetek
│       │   ├── HttpFetcher.java                 # Jsoup HTTP kliens: User-Agent, fejlécek, timeout, hibatűrés
│       │   ├── PropertyParser.java              # Ár, méret, szobaszám és azonosító regex kinyerés és normalizálás
│       │   └── SlugUtils.java                   # Diakritikus ékezetmentesítés és keresési URL slug generálás
│       ├── core/                                # Réteg 2: Tiszta üzleti feldolgozó motorok (Core Processing)
│       │   ├── PropertyFilterEngine.java        # Keresési kritériumok szerinti szűrés (ár, méret, város, megtekintett)
│       │   ├── DeduplicationEngine.java         # Duplikátumszűrés, új találatok azonosítása, snapshot kezelés
│       │   ├── MarketAnalyticsEngine.java       # Piaci KPI és statisztikai számítások (átlagár, m² ár, breakdown)
│       │   └── SearchExecutionEngine.java       # Párhuzamos kereséskoordinátor (Virtual Threads, 15s timeout, 0-crash)
│       ├── model/
│       │   ├── IngatlanItem.java                # Hirdetés adatmodell (gazdag értékobjektumokkal)
│       │   ├── PortalSource.java                # Portálforrás metaadat (azonosító, szín, közvetlen típus)
│       │   ├── PriceInfo.java                   # Strukturált ár (nyers szöveg, numerikus Ft és M Ft)
│       │   ├── PropertyDetails.java             # Részletek (alapterület m², szobaszám, számított Ft/m²)
│       │   ├── SearchCriteria.java              # Keresési paraméterek (városok, árhatárok, típus, aktív portálok)
│       │   └── SearchResult.java                # Keresési aggregáció és portálstatisztikák modellje
│       ├── persistence/                         # Réteg 4: Leválasztott perzisztencia (Decoupled Persistence)
│       │   ├── ListingStorage.java              # Hirdetés-tárolási interfész
│       │   ├── ConfigStorage.java               # Konfiguráció-tárolási interfész
│       │   ├── JsonFileListingStorage.java      # Atomi .tmp + ATOMIC_MOVE JSON tároló ReentrantReadWriteLock-kal
│       │   ├── JsonConfigStorage.java           # Atomi konfiguráció tároló
│       │   ├── LegacyMigrationService.java      # Monolitikus és szöveges napló migráció
│       │   └── IngatlanRepository.java          # Visszafelé kompatibilis Singleton Facade
│       ├── portal/                              # Réteg 3: Külső csatolók és portálmotorok (Pluggable Portal Engines)
│       │   ├── PortalDescriptor.java            # Portál metaadat rekord (név, szín, keresőlink, támogatott típusok)
│       │   ├── PortalEngine.java                # Egységes portálmotor interfész (getDescriptor, scrape)
│       │   ├── PortalEngineRegistry.java        # Dinamikus portálmotor regiszter és provider factory (OCP)
│       │   ├── dh/                              # Duna House (DunaHouseUrlBuilder, DunaHouseParser, DunaHousePortalEngine)
│       │   ├── oc/                              # Otthon Centrum (OtthonCentrumUrlBuilder, OtthonCentrumParser, OtthonCentrumPortalEngine)
│       │   ├── jofogas/                         # Jófogás (JofogasUrlBuilder, JofogasParser, JofogasPortalEngine)
│       │   ├── ingatlanbazar/                   # Ingatlanbazár REST API (IngatlanBazarQueryBuilder, JsonParser, LocationResolver)
│       │   ├── koltozzbe/                       # Költözzbe DOM & JSON-LD (KoltozzbeUrlBuilder, KoltozzbeParser)
│       │   └── ingatlancom/                     # Ingatlan.com Cloudflare fallback (IngatlanComUrlBuilder, Parser)
│       ├── presentation/                        # Réteg 1a: Prezentációs réteg (UI & csatorna izoláció)
│       │   ├── DashboardPresentationService.java# Facade: KPI statisztikák, portál állapotok, aggregáció
│       │   ├── DashboardResponseDto.java        # Gazdagított válaszmodell a felület és kliensek felé
│       │   ├── MarketStatsDto.java              # Piaci KPI mutatók (átlagár, átlagos m² ár, új arány)
│       │   └── PortalStatusDto.java             # Portál egészségügyi és hibaállapot információs kártyák
│       ├── scraper/                             # Visszafelé kompatibilis adapter réteg (IngatlanScraper híd)
│       │   ├── IngatlanScraper.java             # Portálfüggetlen scraper interfész
│       │   ├── BaseScraper.java                 # Absztrakt ősosztály
│       │   ├── PortalScrapeResult.java          # Állapotgazdag és izolált kimenet per portál
│       │   ├── PortalScrapeStatus.java          # Portál futási státusz (SUCCESS, BOT_PROTECTED, stb.)
│       │   └── ScraperRegistry.java             # Legacy regiszter, amely a PortalEngineRegistry-re delegál
│       ├── service/
│       │   └── IngatlanSearchService.java       # Magas szintű keresést vezérlő orchestrator (delegál SearchExecutionEngine-re)
│       └── web/                                 # Réteg 1b: Beágyazott webkiszolgáló és dinamikus UI
│           ├── WebServer.java                   # Beágyazott REST API szerver Java 21 Virtual Threads alapon (/api/portals, CSP)
│           └── WebDashboardHtml.java            # Dinamikus SPA felület shimmer skeletonokkal és portál betöltéssel
```

### Főbb Komponensek és Szerepkörök

* **`WebServer` (`ingatlanok.web`):**
  * Beépített `com.sun.net.httpserver.HttpServer` használata külső függőségek (Tomcat, Jetty, Spring) nélkül.
  * Párhuzamos kéréskezelés `Executors.newVirtualThreadPerTaskExecutor()` segítségével.
  * REST API végpontok:
    * `GET /`: Reszponzív egyoldalas HTML dashboard kiszolgálása.
    * `GET /api/config`: Legutóbbi keresési konfiguráció lekérdezése.
    * `POST /api/search`: Keresés indítása a megadott feltételekkel.
    * `GET /api/items`: Tárolt hirdetések lekérdezése város szerinti szűréssel.
    * `POST /api/clear`: Keresési előzmények és adatbázis törlése.
    * `POST /api/open-all`: Megadott találati URL-ek megnyitása a helyi böngészőben.
    * `GET /api/cities`: Előre definiált magyarországi települések listája.
    * `GET /api/presets`: Gyorsválasztó régiós sablonok (pl. agglomerációk, egyetemvárosok).

* **`IngatlanRepository` (`ingatlanok.persistence`):**
  * Thread-safe Singleton mintát követ (`ConcurrentHashMap`, `newKeySet`).
  * Kétszintű tárolási architektúra:
    1. Városonkénti adatfájlok: `data/cities/<cityKey>.json` a pontos településszintű szegmentáláshoz.
    2. Globális adatbázis: `data/ingatlanok_db.json` az összesített lekérdezésekhez.
  * Automatikus adatmigráció régebbi adatbázis- és szöveges naplóformátumokból (`2017_06_06.txt`, `ingatlanok_history.txt`).

* **`IngatlanSearchService` (`ingatlanok.service`):**
  * Felelős a scraper példányok életciklusáért és koordinált lekérdezéséért.
  * Felügyeli a duplikátumszűrést az `IngatlanRepository`-n keresztül.
  * Rendezi a találatokat ár és dátum szerint, valamint előállítja a portálonkénti találati statisztikákat.

### Projekt Szintű AI Szakértői Skillek (`.agents/skills/`)

A projekt dedikált, verziókövetett Antigravity workspace skillekkel rendelkezik, amelyek az adott szakterület mély ismeretével ruházzák fel a fejlesztést segítő ágenseket:

* **`deep-architect` (`.agents/skills/deep-architect/`):**
  * Mélytervező rendszerszintű architekt és szoftvertervezési specialista.
  * Fókusz: 4 rétegű Clean Architecture, réteg-izoláció, Java 21 Virtual Threads és strukturált konkurencia, 0-Crash elv, pluggable PortalEngine architektúra, leválasztott perzisztencia és Architecture Decision Record (ADR) készítés.
* **`ui-designer` (`.agents/skills/ui-designer/`):**
  * UI/UX designer és beágyazott frontend specialista.
  * Fókusz: `WebDashboardHtml.java` zéró külső függőségű SPA, Tailwind-ihletésű design tokenek, tömeges szűrővezérlők, numerikus stepperek, fajlagos m² ár és méret szerinti rendezés, shimmer loading skeletonok, piaci KPI kártyák, beépített rendszernapló (Log Viewer) és diagnosztikai modal, valamint kliensoldali böngészőlap-nyitás.
* **`security-specialist` (`.agents/skills/security-specialist/`):**
  * Alkalmazásbiztonsági (AppSec) és hardening specialista.
  * Fókusz: Beágyazott `WebServer` localhost kötés és HTTP fejléc védelem (CSP, nosniff, frame-options), konténer hardening (non-root UID 10001 futtatás, kötetjogosultságok), Path Traversal prevenció a perzisztenciában, XSS védelem a hirdetéskártyáknál, ProcessBuilder parancsinjektálás elleni védelem, valamint böngésző URL megnyitási korlátok és sémavalidáció.
* **`database-specialist` (`.agents/skills/database-specialist/`):**
  * Adatbázis és tárolási architektúra specialista.
  * Fókusz: Beágyazott relációs SQLite perzisztencia (`SqliteListingStorage`, `data/ingatlanok.db`), WAL mód (`journal_mode=WAL; synchronous=NORMAL;`), B-Tree indexelési stratégiák (`idx_listings_city`, `idx_listings_portal`, `idx_listings_discovered_at`), tranzakciós batch mentés, atomi JSON lemezre írás (`.tmp` + `ATOMIC_MOVE`), és zökkenőmentes automatikus adatmigráció.
* **`workflow-engineer` (`.agents/skills/workflow-engineer/`):**
  * GitHub Actions és CI/CD automatizálási mérnök és workflow specialista.
  * Fókusz: Professzionális, biztonságos és optimalizált `.github/workflows/` scriptek, jogosultság-minimalizálás (`permissions`), shell injection prevenció, GHCR Docker konténer build és publish szabályok, Docker Buildx GHA gyorstárazás (`cache`), concurrency menedzsment és Thematic Agent PR guard kapuk.
* **`review-and-fix` (`.agents/skills/review-and-fix/`):**
  * Multi-agent minőségbiztosítási, kódvizsgálati és automatikus javítási specialista.
  * Fókusz: Pull Requestek és lokális változtatások átfogó ellenőrzése, a tematikus szakértői kapu (`Thematic Agent Approval Gate`) kiadása, moduláris kapu-validáció és automatikus hibajavítási ciklus (Fix Loop).
* **`devops-specialist` (`.agents/skills/devops-specialist/`):**
  * Konténerizációs, felhő és DevOps rendszermérnök.
  * Fókusz: Dockerfile multi-stage optimalizálás (Maven build + Temurin 21 JRE Alpine runtime), Docker Compose orkesztráció (`docker-compose.yml`), perzisztens kötetek (`./data`, `./logs`), környezeti változók kezelése (`SERVER_HOST`, `SERVER_PORT`), erőforrás-korlátok és fej nélküli (headless/Zero-Server-Desktop-Call) szerveroldali működés.
* **`scraper-reliability-engineer` (`.agents/skills/scraper-reliability-engineer/`):**
  * Web scraping robusztussági és anti-bot megbízhatósági mérnök.
  * Fókusz: Cloudflare WAF és bot-védelem detektálás (HTTP 403/503, Ray ID, captcha minták), modern böngésző ujjlenyomat és Client Hints fejlécek (`Sec-Ch-Ua`), 0-crash elv és portálonkénti graceful fallback, belső REST API és Schema.org JSON-LD parsing, valamint Playwright/Headless sidecar vezérlés.
* **`qa-test-engineer` (`.agents/skills/qa-test-engineer/`):**
  * Minőségbiztosítási, tesztelési és kódlefedettségi mérnök (QA).
  * Fókusz: Rétegzett tesztelési piramis, determinisztikus és 100% offline lefutás, scraper parser tesztek mentett HTML/JSON fixture-ökkel, SQLite `@TempDir` és in-memory integrációs tesztek, WebServer REST integrációs tesztek, és szigorú 80%+ JaCoCo kódlefedettség fenntartása.

---

## 3. Főbb Tervezési Minták és Technikai Döntések

1. **Java 21 Virtual Threads:**
   * A webkiszolgáló és a hálózati I/O műveletek könnyűsúlyú szálakon futnak, így a web scraping hálózati késleltetése nem blokkolja a rendszert.
2. **Polimorf Web Scraping (`Strategy / Template Method` minta):**
   * Minden portál scraper az `IngatlanScraper` interfészt valósítja meg és a `BaseScraper` ősosztályból öröklődik.
   * A letöltést a közös `HttpFetcher` modul biztosítja: realisztikus Chrome User-Agent, referer fejlécek, 15 másodperces hálózati időkorlát és SSL tanúsítványkezelés.
3. **Közös segédeszközök rétege (`ingatlanok.common`):**
   * Ár-, méret- és szobaszám parsing (`PropertyParser`), ékezetmentesítés és slugosítás (`SlugUtils`), településkezelés (`CityUtils`) és asztali integráció (`BrowserUtils`) egy helyre szervezve az SRP és DRY elvek mentén.
4. **0-Crash és Robusztus Graceful Degradation:**
   * Egyetlen weboldal hálózati hibája vagy DOM struktúra-változása sem okozhatja az alkalmazás leállását. A scraperek elkapják a helyi kivételeket, és hibaüzenettel kísért üres találati listát vagy fallback linket szolgáltatnak.
5. **Zéró Külső Frontend Függőség:**
   * A webes felület teljesen önálló, offline is működő Single Page Applicationként van beágyazva a `WebDashboardHtml` osztályba, elkerülve a Node.js/NPM és külső frontend build lépések szükségességét.

---

## 4. Web Scraping Részletek és Portál Kihívások

| Portál | Scraping Módszer | Technikai Részletek és Kihívások |
| :--- | :--- | :--- |
| **Duna House** (`dh.hu`) | Élő Jsoup DOM parsing | URL mintázat: `/elado-ingatlan/<tipus>/<megye>/<varos>/<ar>-mFt`. A hirdetési kártyákból a cím, kikiáltási ár, méret ($m^2$) és szobaszám megbízhatóan kinyerhető. |
| **Otthon Centrum** (`oc.hu`) | Élő Jsoup DOM parsing | URL mintázat query paraméterekkel: `?realestate[ertekesites]=elado&...&<varos>=on`. Modern kártya layout, az árak és paraméterek span elemekből kerülnek feldolgozásra. |
| **Jófogás Ingatlan** (`jofogas.hu`) | Élő Jsoup DOM parsing | Keresési paraméterezés: `?q=<varos>+elado+<tipus>&min_price=...`. A hirdetési listaelemekből regex segítségével tisztítjuk az árat és a címet. |
| **Ingatlan.com** (`ingatlan.com`) | Graceful Bot Fallback | Erős Cloudflare WAF bot-védelem (403 Forbidden / JS challenge). Közvetlen hirdetési keresőlinket generálunk fallbackként, elkerülve a szerver blokkolását. |
| **Ingatlanbazár** (`ingatlanbazar.hu`) | Élő belső REST JSON API | Nyílt és gyors belső REST API (`/api/property-search` és `/api/geo/get-matches`). Pontos árak, alapterület, szobaszám, közvetlen hirdetés linkek és településkód feloldás gyorsítótárazással (`IngatlanBazarLocationResolver`). |
| **Költözzbe** (`koltozzbe.hu`) | Élő DOM & JSON-LD parsing | Kifejezetten eladó családi ház és debreceni agglomeráció fókusz. Pontos árak, alapterület, szobaszám, telekméret és közvetlen hirdetés linkek kinyerése kettős (HTML kártya + Schema.org JSON-LD) mechanizmussal. |

---

## 5. Fejlesztési Irányok és Ütemterv (Roadmap)

### Megvalósult Fejlesztések
- [x] Java 21 LTS környezet és modern függőségek (Maven 3.9+, Jsoup 1.23.2, Gson 2.11.0).
- [x] Modern Web Dashboard beágyazott szerverrel és Virtual Threads kiszolgálással.
- [x] Többvárosos (Multi-city) szűrőrendszer 35+ előre definiált magyar várossal és sablonokkal.
- [x] Kétszintű, városonként szeparált és aggregált JSON hirdetésperzisztencia.
- [x] Vizuális hirdetéskártyák: ár szerinti dinamikus rendezés, szűrés kizárólag az új találatokra.
- [x] Egykattintásos Windows asztali indítók (`start-ui.bat`, `start-ui.ps1`).
- [x] Moduláris projektarchitektúra: közös `common` modul és dedikált `scraper` réteg kialakítása.
- [x] Elavult legacy mock és helper osztályok kivezetése és törlése.
- [x] Strukturált és szintezett naplózási keretrendszer (`AppLogger`, `logs/scraper.log`, `/api/logs` REST végpont, metrikák).
- [x] Portál diagnosztika, hiba felugró ablak és technikai státuszkódok (`PortalStatusDto`, diagnosztikai modal, egykattintásos vágólap export).
- [x] Költözzbe.hu élő ház és debreceni agglomeráció scraper adapter (`KoltozzbeScraper`, kártya és JSON-LD parsing, település és árszűrés).
- [x] Egységes, bolondbiztos asztali indító (`START.bat`), elavult fájlok takarítása, megerősített `.gitignore` és tesztlefedettség növelés (78.75%).
- [x] Otthon Centrum és Ingatlan.com scraper 0 találatos hibáinak elhárítása (#38: oc.hu rejtett autocomplete sablon kiszűrése, gazdag kártya és nagy felbontású kép parsing, Chrome 133 Sec-Ch-Ua fejlécek, robusztus Cloudflare WAF bot-védelem detektálás és egységtesztek, tesztlefedettség növelés 78.99%-ra).
- [x] 4 rétegű moduláris rendszer, pluggable portálmotorok és leválasztott perzisztencia (#52: Core feldolgozó motorok, Pluggable PortalEngine réteg, ListingStorage/ConfigStorage atomi perzisztencia, WebServer biztonsági fejlécek és dinamikus /api/portals UI betöltés shimmer loading skeletonokkal, 191 zöld egység- és integrációs teszt, 79.27% lefedettség).
- [x] Cross-platform Docker konténerizáció és beágyazott relációs SQLite adatbázis (#69: Multi-stage Dockerfile, docker-compose.yml ./data és ./logs kötetekkel, SERVER_HOST/SERVER_PORT konfiguráció, Zero-Server-Desktop-Call kliensoldali böngészőlap-nyitás, org.xerial:sqlite-jdbc WAL móddal és indexekkel, SqliteListingStorage, SqliteConfigStorage, automatikus JSON migráció, maven-shade-plugin fat JAR, 204 zöld egység- és integrációs teszt).
- [x] Automatikus Docker konténer build és publish GitHub Container Registry-be (#80: .github/workflows/docker-publish.yml, develop -> latest, main/release -> fix verzió SemVer/pom.xml alapján expliciten tiltva a lebegő latest felülírását, docker-compose környezeti változós GHCR támogatás, GHA buildx gyorstárazás és formázott Step Summary).
- [x] Docker zéró-konfigurációs indítás és belső nevesített kötetek támogatása (#109: Dockerfile deklaratív VOLUME és konténer-optimalizált JVM flag-ek, docker-compose.yml menedzselt ingatlan_data és ingatlan_logs named volume-ok, leegyszerűsített single-command indítási dokumentáció és CI step summary).
- [x] Egykattintásos GHCR Docker indítókészlet és asztali integráció (#114: start-docker.bat és start-docker.sh zéró-konfigurációs scriptek, automatikus docker pull, fix 'ingatlan-kereso' konténernév, 8080:8080 port forwarding, nevesített kötetek és automatikus böngészőlap-nyitás).

### Tervezett Fejlesztések
- [ ] **Értesítési csatornák:** Telegram Bot API és Discord Webhook integráció az új találatok azonnali továbbításához.
- [ ] **Headless böngésző driver (opcionális modul):** Playwright vagy Selenium integráció a Cloudflare és SPA portálok teljes körű kinyeréséhez.
- [ ] **Időzített háttérfigyelés:** Háttérben futó periodikus ellenőrző szolgáltatás konfigurálható időközökkel.

---

## 6. Munkafolyamat és Verziókezelési Szabályzat (Workflow Rule)

> [!IMPORTANT]
> **Kötelező GitHub PR és Verziókezelési Szabályzat az AI Asszisztens számára:**
>
> 1. **Közvetlen `main` és `develop` ágra commitolás és pusholás szigorúan TILOS:**
>    - A `main` a védett, stabil kiadási ág.
>    - A `develop` a védett integrációs ág.
>    - Minden feladatot új munkavégző ágon (`feature/*`, `fix/*`, `docs/*`, stb.) kell elvégezni, amely a `develop` ágból indul ki.
>
> 2. **Strukturált branch elnevezés (`feature/`, `fix/`, stb.):**
>    - Az ágak neve kötelezően kövesse a típus szerinti struktúrát és angol leírást:
>      - `feature/<rovid-angol-leiras>` – új funkció, felületi vagy logikai fejlesztés
>      - `fix/<rovid-angol-leiras>` – hibajavítás, rendellenesség elhárítása
>      - `docs/<rovid-angol-leiras>` – dokumentáció, szabályzat vagy README módosítása
>      - `refactor/<rovid-angol-leiras>` – kódstruktúra tisztítása, refaktorálás viselkedésváltozás nélkül
>      - `chore/<rovid-angol-leiras>` – build beállítások, függőségek, CI/CD karbantartás
>
> 3. **Mindig új commit a PR-ba:**
>    - Minden változtatáshoz külön, leíró üzenetű új commitot kell készíteni (`git commit -m "..."`).
>    - Tilos már pusholt commitokat felülírni (`--amend` tilos push után); minden módosítás vagy javítás új committal bővítse a branch történetét.
>
> 4. **Angol nyelvű, beszédes PR elnevezés (Title):**
>    - A Pull Request neve **mindig kötelezően angol nyelvű** legyen, és pontosan mutassa be a feladat típusát és tartalmát Conventional Commits formátumban:
>      - Pl.: `feat: add stepper controls and portal counters to dashboard`
>      - Pl.: `fix: resolve timeout issue in jofogas scraper`
>      - Pl.: `docs: update workflow rules with branch structure and label requirements`
>
> 5. **Kötelező GitHub Label (Címke) a PR-on:**
>    - A PR-t kötelező címkével ellátva létrehozni (`gh pr create --label "<label>"`).
>    - Használandó címkék:
>      - `enhancement` – új funkcionalitás vagy UI bővítés esetén
>      - `bug` – hibajavítás esetén
>      - `documentation` – leírások, dokumentáció és szabályzat esetén
>      - Ha a címke még nem létezik a repóban, a `gh label create` paranccsal azonnal létre kell hozni.
>
> 6. **Beszédes, részletes magyar nyelvű összefoglaló (Summary / Description):**
>    - A PR törzse (`--body`) és a chat-válasz záró összefoglalója magyar nyelvű legyen.
>    - Az összefoglalónak kifejezetten **beszédesnek és részletesnek** kell lennie (nem 1 mondatos összegzés), hogy világosan érthető legyen:
>      - **Mi a cél és kontextus:** Miért készült a módosítás?
>      - **Részletes technikai változtatások:** Mely fájlok, komponensek és logikák változtak és hogyan?
>      - **Felhasználói és működési hatás:** Mit tapasztal a felhasználó, hogyan változik a működés?
>      - **Verifikáció és tesztelés:** Hogyan lett ellenőrizve a módosítás helyessége?
>
> 7. **Közvetlen PR Link a válaszban:**
>    - A feladat befejezésekor a beszélgetésben **mindig kötelező közvetlen kattintható linket adni az elkészült GitHub Pull Request-re**.
>
> 8. **Kötelező Tematikus Agent Jóváhagyási Kapu (Thematic Agent Approval Gate & Review-and-Fix):**
>    - Egyetlen Pull Request sem fogadható el és nem fésülhető össze a védett ágakba (`develop`, `main`) anélkül, hogy a módosítás által érintett szakterület kijelölt AI ágense azt jóvá nem hagyta:
>      - **Rendszerarchitektúra & Scraper réteg (`service/`, `scraper/`, `core/`, `portal/`, szálkezelés):** A `deep-architect` és `scraper-reliability-engineer` ágens jóváhagyása kötelező (Clean Architecture, 0-crash garancia).
>      - **Frontend és Felület (`WebDashboardHtml.java`, CSS, JS komponensek):** A `ui-designer` ágens jóváhagyása kötelező (zéró külső függőség, reszponzivitás, UX).
>      - **Biztonság, Hálózat és Folyamatkezelés (`WebServer.java`, `SidecarCliExecutor`, `BrowserUtils`):** A `security-specialist` ágens jóváhagyása kötelező (localhost kötés, CSP, Path Traversal, XSS és command injection védelem).
>      - **Perzisztencia és Adatbázis (`persistence/`, `IngatlanRepository`, `data/`):** A `database-specialist` ágens jóváhagyása kötelező (atomi lemezre írás, ReentrantReadWriteLock, SQLite és duplikátumszűrés).
>      - **CI/CD és GitHub Actions (`.github/workflows/`, `.githooks/`):** A `workflow-engineer` ágens jóváhagyása kötelező (least privilege permissions, shell injection védelem, caching, concurrency).
>      - **Konténerizáció és Futtatási Környezet (`Dockerfile`, `docker-compose.yml`):** A `devops-specialist` ágens jóváhagyása kötelező (multi-stage build, non-root user, perzisztens kötetek).
>      - **Tesztek és Minőségbiztosítás (`src/test/`, JaCoCo):** A `qa-test-engineer` ágens jóváhagyása kötelező (determinisztikus lefutás, 80%+ lefedettség).
>    - A felülvizsgálat lefolytatásához és az észlelt hiányosságok automatikus javításához a `.gemini/commands/review-and-fix.toml` parancs, illetve a `.agents/skills/review-and-fix/` skill használandó.
>    - **Kötelező 1 mondatos indoklás (Rationale Requirement):** A jóváhagyási blokkot **kötelezően a PR leírásának legfelső részeként** kell elhelyezni. Az ágensek nem hagyhatják üresen vagy pusztán jelölőnégyzettel az értékelést: minden szakterületnél kötelező **pontosan 1 tömör mondatban megindokolni**, hogy miért lett `[OK]` (jóváhagyva), `[NOK]` (elutasítva/javítandó), vagy `[N/A]` (nem érintett téma):
>      ```markdown
>      ### 🛡️ Thematic Agent Approval Gate
>      - [x] Architecture (`deep-architect`): [OK] – A Clean Architecture réteghatárok és a 0-crash elv maradéktalanul érvényesül.
>      - [x] UI/UX (`ui-designer`): [N/A] – A módosítás nem érinti a webes dashboardot vagy a frontend kódokat.
>      - [x] Security (`security-specialist`): [OK] – A bemenet-validáció és a localhost szerverbiztonsági fejlécek sértetlenek.
>      - [x] Database (`database-specialist`): [OK] – A szálbiztos tárolás és az atomi perzisztencia garantált.
>      - [x] Workflow/CI (`workflow-engineer`): [OK] – A GitHub Actions munkafolyamatok és a PR guard szabályok rendben vannak.
>      - [x] DevOps (`devops-specialist`): [N/A] – Konténerizációs vagy futtatási fájlok nem módosultak.
>      - [x] Scraper Reliability (`scraper-reliability-engineer`): [N/A] – Web scraping és portálmotorok nem érintettek.
>      - [x] QA/Testing (`qa-test-engineer`): [OK] – Minden automatizált egység- és integrációs teszt hibátlanul lefut.
>      ```
>    - A `.github/workflows/pr-guard.yml` CI munkafolyamat automatikusan elemzi a diffet, és meghiúsítja az ellenőrzést, ha a szükséges tematikus ágens jóváhagyása hiányzik.
>
> 9. **Szigorú PR Javítási Ciklus Zöld CI-ig (Strict Fix Loop Until Green CI):**
>    - Ha a Pull Requesten futó bármelyik CI pipeline, PR Guard vagy teszt elbukik (piros státusz), a feladatot **tilos késznek nyilvánítani vagy a felhasználóra hagyni a hibajavítást**.
>    - Az ágens köteles azonnal megkezdeni a **Fix Loop** ciklust:
>      1. A hiba pontos okának feltárása a CI és workflow futási naplók (`gh run view --log-failed`) vagy helyi vizsgálat alapján.
>      2. A szükséges korrekciók elvégzése a kódban, tesztekben vagy konfigurációban.
>      3. Dedikált, leíró üzenetű új commit létrehozása (`git commit -m "fix: ..."`) és azonnali feltöltése (`git push origin <branch>`).
>      4. A CI futások újraellenőrzése mindaddig, amíg **az összes pipeline ellenőrzés kivétel nélkül 100%-ban zöld nem lesz**.
>    - Egyetlen PR sem tekinthető befejezettnek sikertelen vagy függőben lévő CI ellenőrzés mellett.
>
> 10. **PR Beolvasztás / Merge Szigorú Tilalma (Never Merge Pull Requests):**
>    - Az AI asszisztens **soha, semmilyen körülmények között nem mergelheti be a Pull Requesteket** a felhasználó nevében (`gh pr merge` vagy közvetlen git merge futtatása szigorúan tilos).
>    - A feladat hatóköre a kódolásnál, tesztelésnél, commitolásnál, pusholásnál, a PR létrehozásánál és a zöld CI státusz eléréséig tart. A PR átvizsgálása, jóváhagyása és beolvasztása kizárólag a humán felhasználó / repó tulajdonos jogköre.
>
> 11. **Kapcsolódó Hibajegyek / Ticketek Automatikus Lezárása (Closing Linked Issues):**
>    - Ha a Pull Request egy meglévő issue-t / ticketet valósít meg vagy javít, **a PR leírásában kötelező használni a GitHub hivatalos összekapcsoló kulcsszavait**:
>      - `Closes #<issue_number>`
>      - `Fixes #<issue_number>`
>      - `Resolves #<issue_number>`
>    - A GitHub beépített mechanizmusa révén, amikor a PR jóváhagyásra és összefésülésre kerül (`merged`) a védett ágba (`develop` vagy `main`), a hivatkozott issue automatikusan lezárásra (`CLOSED`) kerül.
>    - A PR létrehozásakor (`gh pr create`) a `--body` mező végén vagy a dedikált *Kapcsolódó Hibajegyek (Related Issues)* blokkban mindig szerepeljen a pontos hivatkozás (pl. `Closes #93`).



