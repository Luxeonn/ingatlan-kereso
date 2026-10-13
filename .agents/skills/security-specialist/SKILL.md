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

Ez a skill a szoftverbiztonsági auditok, a bemenet-ellenőrzés, a beágyazott webszerver védelem és a biztonságos külső folyamatkezelés előírásait rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Veszélyeztetettségi Modell és Támadási Felületek (Threat Modeling)

Az alkalmazás egy helyi asztali környezetben futó, beágyazott HTTP REST szervert használ. Bár alapvetően lokális felhasználásra tervezett, az alábbi kiemelt támadási felületekkel és kockázatokkal kell számolni:

1. **Beágyazott HTTP Port (Port 8080):** Helyi hálózatról (LAN) vagy helyi rosszindulatú szoftverek által indított kérések.
2. **Web Scraping Bemenetek:** Külső weboldalakról érkező nem megbízható HTML/szöveges tartalom (pl. XSS injektálás a hirdetés címeiben).
3. **Fájlrendszer I/O:** Felhasználói paraméterekből képzett fájlnevek (`data/cities/<cityKey>.json`) és az ebből fakadó Path Traversal kockázat.
4. **Alfolyamat Végrehajtás (Process Execution):** A Python CLI Sidecar hívása (`ProcessBuilder`) és a parancsinjektálás kockázata.
5. **Böngésző Integráció:** Külső linkek megnyitása (`Desktop.browse` / `ProcessBuilder`) és a kártékony URI sémák kockázata.

---

## 2. Biztonsági Követelmények és Megvalósítási Szabályok

### 2.1 Beágyazott HTTP Szerver Hardening (`WebServer.java`)
* **Localhost Binding:** Alapértelmezetten kizárólag a helyi interfészre hallgasson: `InetSocketAddress("127.0.0.1", port)`. Soha ne kösd alapértelmezetten a `0.0.0.0` címre!
* **HTTP Metódus Ellenőrzés:**
  - Minden végpontnál szigorúan ellenőrizd a megengedett metódust (`GET`, `POST`).
  - Illetéktelen vagy váratlan metódusok esetén azonnal `405 Method Not Allowed` választ kell adni.
* **Biztonsági Fejlécek (Security Headers):**
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Content-Security-Policy: default-src 'self' 'unsafe-inline'; img-src 'self' data: https:;`

### 2.2 Path Traversal Megelőzés (`IngatlanRepository.java`)
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

### 2.3 Cross-Site Scripting (XSS) Prevenció a Frontendben (`WebDashboardHtml.java`)
* A web scraping során kinyert szövegek (hirdetés címe, leírása, ára, portál megjegyzések) megbízhatatlan forrásból származnak!
* A frontend DOM manipuláció során:
  - Szöveges tartalomhoz kizárólag `element.textContent` használandó `element.innerHTML` helyett.
  - Ha HTML sablon-literálokat használsz, minden dinamikus mezőt meg kell tisztítani egy beépített `escapeHtml()` függvénnyel:
    ```javascript
    function escapeHtml(str) {
      if (!str) return '';
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }
    ```

### 2.4 Command Injection és Process Execution Biztonság (`SidecarCliExecutor.java`)
* Soha ne indíts folyamatot shell wrapperen keresztül (`cmd.exe /c` vagy `bash -c`) összefűzött parancssorral!
* Mindig a `ProcessBuilder(List<String> command)` argumentumlistás konstruktorát használd.
* Minden átadott argumentumot (város, ár, típus) paraméter-szinten validálj és tisztíts meg futtatás előtt.
* Időtúllépés (`process.waitFor(20, TimeUnit.SECONDS)`) után kényszerített leállítás (`process.destroyForcibly()`) a háttérben ragadó zombi-folyamatok ellen.

### 2.5 Biztonságos Böngésző Megnyitás (`BrowserUtils.java`)
* Csak és kizárólag `http://` és `https://` protokollal kezdődő URL-ek nyithatóak meg!
* Szigorúan tilts le minden más sémát (`file://`, `javascript:`, `cmd:`, `powershell:`, `ms-settings:`).
* Lap-túlcsordulás elleni védelem: Maximalizáld az egyszerre megnyitható lapok számát (alapértelmezetten max. 10 lap), hogy megvédd a rendszert a memória-túlterheléstől és az operációs rendszer lefagyásától.

---

## 3. Biztonsági Audit Ellenőrző Lista

A részletes ellenőrzési pontokat a [Biztonsági Audit Ellenőrző Listában](./references/security-audit-checklist.md) találod.
