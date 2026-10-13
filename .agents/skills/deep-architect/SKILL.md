---
name: deep-architect
description: >-
  Mélytervező rendszerszintű architekt és szoftvertervezési specialista. Használd ezt a skillt,
  ha a felhasználó rendszerarchitektúra tervezését, mély refaktorálást, új alrendszerek bevezetését,
  komponens-határok kijelölését, Java 21 konkurens és aszinkron minták (Virtual Threads, Sidecar IPC)
  tervezését, vagy Architecture Decision Record (ADR) készítését kéri az ingatlan-kereso projektben.
metadata:
  icon: architecture
---

# Mélytervező Architekt Skill (Deep Systems & Software Architect)

Ez a skill a projekt mélyreható szoftver- és rendszertervezési döntéseit, komponens-felelősségeit, tervezési mintáit és architekturális szabályait rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Architektúra Alapelvek és Filozófia

### 1.1 Clean Architecture és Réteg-izoláció
Az alkalmazás szigorúan rétegzett és laza csatolású (`Loose Coupling`, `High Cohesion`):
1. **Model / Domain réteg (`ingatlanok.model`):**
   - Gazdag értékobjektumok (`PriceInfo`, `PropertyDetails`, `IngatlanItem`, `SearchCriteria`, `SearchResult`).
   - Nem tartalmazhat semmilyen I/O műveletet, Jsoup vagy HTTP függőséget.
2. **Common / Utility réteg (`ingatlanok.common`):**
   - Általános, újrafelhasználható, állapotmentes segédosztályok (`HttpFetcher`, `PropertyParser`, `SlugUtils`, `CityUtils`, `BrowserUtils`).
3. **Scraper réteg (`ingatlanok.scraper`):**
   - Strategy / Template Method minta: `IngatlanScraper` interfész, `BaseScraper` közös alap, `ScraperRegistry` dinamikus felderítés.
   - Hibrid architektúra: Szükség esetén külső folyamat sidecar delegálás (`SidecarCliExecutor`).
   - Minden scraper izolált: egyetlen portál hibája vagy struktúraváltozása sem ránthatja le a rendszert (`0-Crash` elv).
4. **Service / Orchestration réteg (`ingatlanok.service`):**
   - `IngatlanSearchService`: Összefogja a párhuzamos scraper hívásokat, koordinálja a duplikátumszűrést a perzisztencia rétegen keresztül, és aggregálja a piaci KPI statisztikákat.
5. **Persistence réteg (`ingatlanok.persistence`):**
   - `IngatlanRepository`: Thread-safe singleton, elszigeteli a JSON/fájl I/O vagy későbbi SQL adatbázis műveleteket az üzleti logikától.
6. **Presentation / Web réteg (`ingatlanok.presentation`, `ingatlanok.web`):**
   - `DashboardPresentationService`: Facade DTO átalakító (KPI statisztikák, portál állapotok).
   - `WebServer`: Beágyazott, függőségmentes HTTP API `Virtual Threads` háttérrel.
   - `WebDashboardHtml`: Beágyazott Single Page Application (SPA).

---

## 2. Döntési és Tervezési Minták

### 2.1 Java 21 Virtual Threads és Konkurencia
* Használj `Executors.newVirtualThreadPerTaskExecutor()`-t minden I/O-intenzív feladatra (web scraping, HTTP API kérések, hálózati műveletek).
* Kerüld a `synchronized` blokkokon belüli blokkoló I/O-t (Carrier Thread pinning megelőzése); helyette használj `ReentrantLock`-ot vagy nem blokkoló adatstruktúrákat (`ConcurrentHashMap`, `CompletableFuture`).

### 2.2 Scraper Extensibility (Open/Closed Principle)
Új portál integrálásakor:
1. Hozz létre egy új osztályt a `ingatlanok.scraper` csomagban, amely kiterjeszti a `BaseScraper`-t.
2. Regisztráld a `PortalSource` enum-ban a portál metaadatait (név, URL, badge szín).
3. Regisztráld a `ScraperRegistry` szolgáltatásban.
4. Biztosíts graceful fallback mechanizmust és 0-crash viselkedést.

### 2.3 Hibrid Java-Sidecar Minta (Anti-Bot / TLS Fingerprinting)
Amikor a Jsoup / Java natív HTTP kliens elakad WAF vagy Cloudflare védelem miatt (pl. JA3/JA4 ujjlenyomat-ellenőrzés):
* Alkalmazz könnyűsúlyú on-demand CLI sidecart (`tools/sidecar.py` vagy előre fordított bináris).
* A kommunikáció szigorúan stdout-on keresztül történjen JSON formátumban.
* Szigorú timeout (max. 20 mp) és process lifecycle felügyelet.
* Ha a sidecar nem futtatható vagy hiányzik, zökkenőmentesen álljon vissza közvetlen böngésző keresési link generálására (`BOT_PROTECTED` státusz).

---

## 3. Architektúra Döntési Munkafolyamat (ADR Workflow)

Amikor jelentős rendszermódosítást tervezel:
1. **Kontextus és Probléma feltárása:** Miért nem alkalmas a jelenlegi megoldás?
2. **Alternatívák elemzése:** Legalább 2-3 opció kidolgozása pro/kontra táblázattal (teljesítmény, karbantarthatóság, függőségek, komplexitás).
3. **Döntési javaslat:** Választott stratégia és annak indoklása.
4. **Következmények (Trade-offs):** Milyen kompromisszumokkal jár a döntés?
5. **ADR dokumentáció:** Használd a [Kapcsolódó ADR sablont](./references/architecture-decision-record-template.md).

---

## 4. Architektúra Ellenőrző Lista (Architect's Checklist)

- [ ] **Függőségi irány:** A belső rétegek (Domain/Model) nem függenek külső rétegektől (Web/UI/Scraper)?
- [ ] **0-Crash garancia:** Hálózati hiba vagy DOM változás esetén az alkalmazás stabil marad?
- [ ] **Szálbiztosság:** A megosztott állapotok thread-safe tárolókban (`ConcurrentHashMap`, `AtomicBoolean`) vannak?
- [ ] **Erőforrás-gazdálkodás:** Minden `InputStream`, `OutputStream`, HTTP kapcsolat és Process le van zárva (`try-with-resources`)?
- [ ] **Tesztelhetőség:** Az üzleti logika függetlenül egységtesztelhető mock adatokkal?
