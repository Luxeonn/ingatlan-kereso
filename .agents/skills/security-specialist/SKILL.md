---
name: security-specialist
description: >-
  Alkalmazásbiztonsági (AppSec) és rendszerhardening specialista. Használd ezt a skillt biztonsági
  felülvizsgálatokhoz, sebezhetőségi auditokhoz, beágyazott HTTP szerver hardeninghez (CORS, CSP, localhost bind),
  bemenet-validációhoz, Path Traversal, XSS, SSRF, Command Injection megelőzéshez, valamint a Python Sidecar
  folyamatfuttatás és a böngészőintegráció biztonságos megvalósításához.
metadata:
  icon: shield
---

# Biztonsági Specialista Skill (AppSec & System Hardening)

Ez a skill a szoftverbiztonsági auditok, a bemenet-ellenőrzés, a beágyazott webszerver védelem, a konténer-hardening és a biztonságos külső folyamatkezelés előírásait rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Veszélyeztetettségi Modell és Támadási Felületek (Threat Modeling)

Az alkalmazás egy asztali vagy Docker konténer környezetben futó, beágyazott HTTP REST szervert használ. Az alábbi kiemelt támadási felületekkel és kockázatokkal kell számolni:

1. **Beágyazott HTTP Port (Port 8080):** Helyi hálózatról (LAN), más konténerekből vagy helyi rosszindulatú kódok által indított kérések.
2. **Konténer Jogosultságok és Fájlrendszer:** Root felhasználóként való futtatás kockázata konténer környezetben.
3. **Web Scraping Bemenetek:** Külső weboldalakról érkező nem megbízható HTML/szöveges tartalom (pl. XSS injektálás a hirdetés címeiben vagy leírásában).
4. **Fájlrendszer I/O:** Felhasználói paraméterekből képzett fájlnevek (`data/cities/<cityKey>.json`, SQLite DB útvonalak) és az ebből fakadó Path Traversal kockázat.
5. **Alfolyamat Végrehajtás (Process Execution):** Külső folyamat / Sidecar hívása (`ProcessBuilder`) és a parancsinjektálás kockázata.
6. **Böngésző Integráció:** Külső linkek megnyitása (`Desktop.browse` / `window.open`) és a kártékony URI sémák kockázata.

---

## 2. Biztonsági Követelmények és Megvalósítási Szabályok

### 2.1 Beágyazott HTTP Szerver Hardening (`WebServer.java`)
* **Hálózati Címhez Kötés (Host Binding):**
  - Asztali natív futtatásnál alapértelmezetten kizárólag a helyi interfészre hallgasson: `InetSocketAddress("127.0.0.1", port)`.
  - Docker konténerben a `SERVER_HOST=0.0.0.0` környezeti változó engedélyezett a konténerhíd miatt, de a host oldalon szigorúan a `127.0.0.1:8080:8080` port-mapping javasolt privát hozzáféréshez.
* **HTTP Metódus Ellenőrzés:**
  - Minden végpontnál szigorúan ellenőrizd a megengedett metódust (`GET`, `POST`).
  - Illetéktelen vagy váratlan metódusok esetén azonnal `405 Method Not Allowed` választ kell adni.
* **Biztonsági Fejlécek (Security Headers):**
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Content-Security-Policy: default-src 'self' 'unsafe-inline'; img-src 'self' data: https:;`

### 2.2 Konténer Hardening és Izoláció (`Dockerfile`, `docker-compose.yml`)
* **Non-Root Felhasználó:** A konténerben a futtató felhasználó szigorúan nem root:
  ```dockerfile
  RUN addgroup -g 10001 -S appgroup && adduser -u 10001 -S appuser -G appgroup
  USER appuser
  ```
* **Kötet Jogosultságok:** Csak a perzisztens könyvtárak (`/app/data`, `/app/logs`) rendelkezzenek írási joggal a non-root felhasználó számára (`chown -R appuser:appgroup`).
* **Zero Secrets in Image:** A Dockerfile rétegek és környezeti változók soha ne tartalmazzanak privát kulcsokat, tokeneket vagy jelszavakat.
* **Capability Drop:** Docker compose vagy Kubernetes futtatásnál ajánlott a felesleges Linux kernel képességek eldobása (`cap_drop: [ALL]`).

### 2.3 Path Traversal Megelőzés (`persistence` & `common`)
Amikor felhasználói bemenet alapján (pl. városnév vagy `cityKey`) fájlt hozunk létre vagy olvasunk a `data/cities/` mappából:
1. **Szigorú kulcs-tisztítás:** A településkulcs csak kisbetűket, számokat és kötőjelet tartalmazhat (`^[a-z0-9_-]+$`).
2. **Útvonal Normalizálás és Határellenőrzés:**
   ```java
   Path basePath = Paths.get("data", "cities").toAbsolutePath().normalize();
   Path targetPath = basePath.resolve(cityKey + ".json").normalize();
   if (!targetPath.startsWith(basePath)) {
       throw new SecurityException("Érvénytelen elérési út: " + cityKey);
   }
   ```
3. A `..` szekvenciát és az elválasztó karaktereket (`/`, `\`) azonnal el kell utasítani.

### 2.4 Cross-Site Scripting (XSS) Prevenció a Frontendben (`WebDashboardHtml.java`)
* A web scraping során kinyert szövegek (hirdetés címe, leírása, ára, portál megjegyzések, log sorok) megbízhatatlan forrásból származnak!
* A frontend DOM manipuláció során:
  - Szöveges tartalomhoz kizárólag `element.textContent` használandó `element.innerHTML` helyett.
  - Ha HTML sablon-literálokat használsz, minden dinamikus mezőt kötelező megtisztítani az `escapeHtml()` függvénnyel.

### 2.5 Command Injection és Biztonságos Böngésző Megnyitás (`BrowserUtils.java`)
* **Parancsinjektálás elleni védelem:** Soha ne használj shell wrappert (`cmd.exe /c`, `sh -c`). Mindig a `ProcessBuilder(List<String>)` argumentumlistát használd.
* **Böngésző URL Sémaellenőrzés:**
  - Csak és kizárólag `http://` és `https://` protokollal kezdődő URL-ek nyithatóak meg!
  - Szigorúan tilos bármilyen más séma (`file://`, `javascript:`, `cmd:`, `powershell:`).
  - Lap-túlcsordulás elleni védelem: Maximalizáld az egyszerre megnyitható lapok számát (alapértelmezetten max. 10 lap).

---

## 3. Biztonsági Audit Ellenőrző Lista

A részletes ellenőrzési pontokat a [Biztonsági Audit Ellenőrző Listában](./references/security-audit-checklist.md) találod.
