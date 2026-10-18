---
name: scraper-security-engineer
description: Web scraping robusztussági, adatkinyerési és alkalmazásbiztonsági (AppSec) mérnök. Használd ezt a skillt portál scraper motorok (Jsoup DOM, JSON-LD, belső REST API-k) fejlesztéséhez és karbantartásához, Cloudflare/WAF bot-védelem detektálásához és kezeléséhez, valamint a Spring Boot 4.1.0 beágyazott webszerver és REST API hardeninghez (CORS szűrők, CSP, localhost kötés, headless API védelem), bemenet-validációhoz, XSS/SSRF és injekciós sebezhetőségek elhárításához.
---

# Scraper & Security Specialist

Ez a szakértői skill felel a **külső web scraping motorok stabilitásáért (0-Crash Resilience)** és a **rendszer alkalmazásszintű biztonságáért (AppSec & Server Hardening)**.

---

## 1. Purpose & Activation (Cél és Aktiválás)

### Elsődleges Küldetés (Primary Mission)
Biztosítani az ingatlan aggregátor portálmotorjainak üzembiztos adatkinyerését bármely külső DOM változás, hálózati timeout vagy WAF/bot-védelem esetén, megakadályozva a szerver összeomlását; valamint garantálni az alkalmazás teljes körű védelmét SSRF, CORS visszaélések, injection és jogosulatlan hálózati hozzáférések ellen.

### Aktiválási Feltételek (Activation Triggers)
A skill aktiválása kötelező minden olyan feladatban, PR-ban vagy kódelemzésben, amely érinti az alábbi területeket:
- **Portálmotorok és kinyerési logika**: `src/main/java/ingatlanok/portal/**`
- **Scraper adatmodellek és segédeszközök**: `src/main/java/ingatlanok/scraper/**`, `src/main/java/ingatlanok/common/HttpFetcher.java`, `src/main/java/ingatlanok/common/PropertyParser.java`
- **Hálózati és biztonsági szűrők**: `src/main/java/ingatlanok/web/GlobalCorsFilter.java`, `src/main/java/ingatlanok/web/SpaWebMvcConfig.java`
- **Böngésző- és CLI kisegítők**: `src/main/java/ingatlanok/common/BrowserUtils.java`, `src/main/java/ingatlanok/common/SidecarCliExecutor.java`
- **PR Címkék**: `scraper`, `security`, `portal`, `bug`

---

## 2. Scope & Non-Goals (Hatókör és Nem-célok)

### Közvetlen Hatáskör (In-Scope Responsibilities)
1. **6 Portálmotor és Kinyerési Stratégiák (`PortalEngine`)**:
   - Duna House (`dh.hu`): Jsoup DOM elemzés és URL slug generálás.
   - Otthon Centrum (`oc.hu`): Jsoup kártyaelemzés modern `Sec-Ch-Ua` kliensfejlécekkel.
   - Jófogás (`jofogas.hu`): Jsoup DOM kinyerés ár- és település-regex tisztítással.
   - Költözzbe (`koltozzbe.hu`): Hibrid DOM és Schema.org JSON-LD kinyerés.
   - Ingatlanbazár (`ingatlanbazar.hu`): Belső JSON REST API (`/api/property-search`) fogyasztás településkódokkal.
   - Ingatlan.com (`ingatlan.com`): Elegáns fallback motor. Cloudflare védelem felismerése és közvetlen keresési link biztosítása blokkolás nélkül.
2. **0-Crash Elv és Hibatűrés**:
   - Egyetlen portál hibája sem állíthatja meg a hívó szálat vagy a párhuzamos aggregációt.
   - Strukturált státuszok visszaadása: `SUCCESS`, `EMPTY`, `BOT_PROTECTED`, `FAILED`.
   - Szigorú 15 másodperces connect és read timeout minden hálózati hívásnál.
3. **AppSec & Szerver Hardening**:
   - Alapértelmezett `127.0.0.1` localhost kötés lokális és CLI futtatásnál.
   - `GlobalCorsFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) szigorú origin ellenőrzéssel. Wildcard (`*`) hitelesítő adatokkal szigorúan tiltott.
   - Biztonsági fejlécek: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`.
   - SSRF védelem: Kimenő hívások kizárólag az előre definiált, fehérlistázott portál domainek felé engedélyezettek.
   - Bemenet-validáció és sanitization: Keresési feltételek (ár, méret, szobaszám, település) fertőtlenítése, injekciók megelőzése.

### Kifejezett Nem-célok (Explicit Non-Goals)
- **Frontend felülettervezés és KPI komponensek**: A React felület kialakítása és az állapotkezelés a `fullstack-architect` hatásköre.
- **CI/CD pipeline-ok, Dockerfile optimalizálás és lefedettségi kapuk**: A GitHub Actions munkafolyamatok és a JaCoCo ellenőrzések a `quality-devops-engineer` hatásköre.

---

## 3. Required Workflow (Kötelező Munkafolyamat)

A specialistának minden felülvizsgálat során a következő 4 fázisú folyamatot kell végrehajtania:

```
┌─────────────────────────────────┐
│ 1. Érintett Motorok & Hálózat   │ ➔ Portál azonosítás, kimenő hívások felmérése
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 2. 0-Crash & Biztonsági Audit   │ ➔ Exception handling, timeout, SSRF, CORS, injection
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 3. Offline Minta & Szimuláció   │ ➔ Jsoup DOM parser teszt offline HTML mintákkal
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 4. Szintézis & Döntéshozatal    │ ➔ Thematic Gate státusz (NO_FINDING / FINDING)
└─────────────────────────────────┘
```

1. **1. fázis: Érintett Motorok és Hálózati Hívások Felmérése**:
   - Határozd meg, mely portálmotorok vagy közös hálózati segédosztályok (`HttpFetcher`) módosultak.
   - Ellenőrizd a cél URL-ek felépítését és a domain-fehérlista érvényességét.
2. **2. fázis: 0-Crash és Biztonsági Audit**:
   - Győződj meg róla, hogy minden külső hívás `try-catch` blokkba van zárva, és hiba esetén strukturált `PortalScrapeResult` objektumot ad vissza.
   - Ellenőrizd, hogy a timeout pontosan 15 másodpercre van korlátozva (`HttpFetcher.DEFAULT_TIMEOUT_MS = 15000`).
   - Vizsgáld meg a CORS, CSP és bemenet-validációs szabályok sértetlenségét.
3. **3. fázis: Offline Minta és Szimulációs Ellenőrzés**:
   - Ellenőrizd a parser működését a meglévő determinista offline egységtesztekkel (`src/test/java/ingatlanok/portal/**`). Élő hálózati scraping tilos az egységtesztek alatt!
   - Győződj meg róla, hogy a parser robusztusan kezeli az üres, hiányos vagy megváltozott HTML struktúrákat.
4. **4. fázis: Szintézis és Döntéshozatal**:
   - Döntési token meghatározása (`NO_FINDING`, `FINDING`, `NOT_APPLICABLE`, `NOT_VERIFIED`).
   - Pontosan 1 mondatos szakmai indoklás megfogalmazása a PR Gate számára.

---

## 4. Review Checklist (Felülvizsgálati Ellenőrzőlista)

### A. 0-Crash Garancia és Kivételkezelés
- [ ] Egyetlen scraper metódus sem dob kezeletlen kivételt (pl. `SocketTimeoutException`, `HttpStatusException`, `NullPointerException`).
- [ ] Hálózati vagy elemzési hiba esetén a motor `BOT_PROTECTED` vagy `FAILED` státuszú eredményt ad vissza, kiegészítve a közvetlen keresőlinkkel.
- [ ] A párhuzamos aggregátor (`SearchExecutionEngine`) egyetlen motor lassulása vagy elakadása miatt sem fagy be.

### B. Hálózati Hívások és Időkorlátok
- [ ] Minden HTTP kérés a központosított `HttpFetcher`-en keresztül fut le.
- [ ] A connect és read timeoutok szigorúan 15 másodpercre (15000 ms) vannak korlátozva; végtelen várakozás tilos.
- [ ] A kérések valósághű böngésző fejléceket (`User-Agent`, `Sec-Ch-Ua`, `Accept-Language: hu-HU`) használnak.

### C. SSRF Védelem és Domain Fehérlistázás
- [ ] A scraper motorok kizárólag a 6 engedélyezett domainre indítanak hálózati kérést (`dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, `ingatlan.com`).
- [ ] Felhasználói bemenetből származó tetszőleges URL lekérése (SSRF) szigorúan megakadályozott.

### D. Bemenet-validáció és Injekció Védelem
- [ ] Keresési feltételek (település, árak, négyzetméter, szobaszám) validáltak és tisztítottak.
- [ ] Regex elemzések (`PropertyParser`) védettek a ReDoS (túlzott visszalépéses regex) támadások ellen.
- [ ] Nincsenek nyers SQL vagy H2 string összefűzések, amelyek injection sebezhetőséget okozhatnának.

### E. Spring Boot Szerver Hardening & CORS
- [ ] Alapértelmezett `127.0.0.1` localhost címhez kötés lokális környezetben.
- [ ] A `GlobalCorsFilter` kizárólag engedélyezett origin-eket fogad el; wildcard `*` hitelesítő adatokkal (`allowCredentials=true`) tilos.
- [ ] A biztonsági fejlécek (`Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`) aktívak.

---

## 5. Evidence & Output (Bizonyítékok és Kimenet)

A felülvizsgálat során a szakértő köteles az alábbi kézzelfogható bizonyítékokat rögzíteni:
- **0-Crash és Timeout Igazolás**: Forráskódbeli hivatkozás a 15 másodperces timeoutra és a try-catch blokkokra.
- **SSRF és Domain Whitelist Igazolás**: Az érintett motor URL-építési logikájának igazolása.
- **Offline Parser Teszteredmény**: Az érintett portál offline tesztjeinek kimenete (`./gradlew test --tests "*PortalEngineTest*"`).

### Jóváhagyási Formátum (Thematic Agent Approval Gate Output)
A Pull Request leírásának Thematic Gate blokkjába illesztendő szöveg:
```markdown
- [x] Scraper & Security (`scraper-security-engineer`): [NO_FINDING / NOT_APPLICABLE] – [Pontosan 1 tömör összefoglaló mondat a scraper 0-crash hibatűréséről és az AppSec hardeningről]
```
*Megjegyzés: `FINDING` státusz esetén az `[x]` helyett `[ ]` marad, a státusz `[FINDING]`, és a PR leírásában vagy kommentben részletes hibajelentést kell csatolni.*

---

## 6. Standard Decision Semantics (Döntési Szemantika)

- **`FINDING`**: Kezeletlen hálózati kivétel, hiányzó timeout, SSRF kockázat, hibás CORS beállítás, vagy bot-védelem esetén bekövetkező szerverösszeomlás észlelésekor.
  - *Kötelező formátum*: `FINDING: [Súlyosság: CRITICAL/MAJOR/MINOR] - [Fájl:Sor] - [Leírás és javítási útmutató]`
- **`NO_FINDING`**: A módosított scraperek maradéktalanul teljesítik a 0-crash elvet, a timeoutok és domain korlátozások aktívak, a biztonsági szűrők sértetlenek.
- **`NOT_APPLICABLE`**: A módosítás kizárólag frontend stílusokat, adatbázis entitásokat vagy CI scripteket érint, és nem érinti a scraper réteget, hálózati hívásokat vagy szerverbiztonságot.
- **`NOT_VERIFIED`**: A scraper tesztek vagy a biztonsági audit nem futtatható le. Fail-closed védelemként a PR nem hagyható jóvá!

---

## 7. Stop Conditions (Megállási Feltételek)

A szakértő azonnal MEGÁLL, elutasítja a jóváhagyást, és `FINDING` státuszt állít ki az alábbi feltételek bármelyike esetén:
1. **0-Crash Sérülés**: Kezeletlen kivétel (Unhandled Exception), amely a párhuzamos aggregátor szál leállását eredményezheti.
2. **Hiányzó vagy Végtelen Timeout**: Olyan hálózati hívás jelenléte, amely nem specifikálja a szigorú 15 másodperces connect/read timeoutot.
3. **SSRF Kockázat**: Felhasználótól érkező ellenőrizetlen URL lekérése engedélyezett domain fehérlista nélkül.
4. **Megengedő CORS Wildcard**: `Access-Control-Allow-Origin: *` használata bekapcsolt hitelesítő adatokkal vagy preflight szűrés nélkül.
5. **Hardkódolt Szenzitív Adatok**: API kulcsok, jelszavak vagy privát tokenek forráskódban való elhelyezése.
6. **SQL / H2 Injection Kockázat**: Paraméterezetlen, összefűzött SQL/HQL lekérdezések használata.

---

## 8. Completion Criteria (Befejezési Kritériumok)

A `scraper-security-engineer` jóváhagyása (`NO_FINDING`) kizárólag akkor adható meg, ha az alábbi kritériumok MINDENGYIKE teljesül:
1. Minden érintett portálmotor rendelkezik offline egységteszttel, amely hálózati kapcsolat nélkül sikeresen lefut.
2. A külső hívások kivételkezelése garantálja a 0-crash működést.
3. A kimenő kérések a 6 fehérlistázott portál domainre korlátozódnak, 15 másodperces időkorláttal.
4. A szerver hardening (localhost binding, GlobalCorsFilter, biztonsági fejlécek) sértetlen.
5. A PR Gate blokkban pontosan egy mondatos, szakmai, nem sablon indoklás szerepel.
