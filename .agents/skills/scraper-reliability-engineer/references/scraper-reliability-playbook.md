# Scraper Megbízhatósági és Hibaelhárítási Forgatókönyv

Ez a dokumentum a portálok scraping hibáinak azonosításához és gyors elhárításához nyújt gyakorlati útmutatást.

---

## 1. Tipikus Hibák és Megoldási Minták

### 1.1 "0 találat" vagy Üres Lista Eredmény
* **Lehetséges okok:**
  1. A portál megváltoztatta a kártyák HTML konténerének CSS osztályát (pl. `.listing-card` helyett `.property-item-card`).
  2. Az oldal rejtett JavaScript sablonokat tartalmaz a DOM-ban (pl. Autocomplete vagy Skeleton elemek), amelyeket a szelektor tévesen találatnak hisz.
  3. A keresési URL query paraméterei módosultak.
* **Elhárítási lépések:**
  - Töltsd le a nyers HTML-t böngészőből vagy `curl` segítségével.
  - Vizsgáld meg a DOM fát és ellenőrizd az egyedi kártyaazonosítókat (`data-id`, `data-property-id`).
  - Hozz létre offline HTML fixture fájlt a tesztekhez (`src/test/resources/`), és futtasd le az egységtesztet.

### 1.2 HTTP 403 Forbidden / Cloudflare Challenge
* **Lehetséges okok:**
  - A Jsoup alapértelmezett TLS kérése nem felel meg a modern böngészők JA3/JA4 ujjlenyomatának.
  - A kérések gyakorisága kiváltotta az IP alapú sebességkorlátozást (Rate Limiting).
* **Elhárítási lépések:**
  - Állítsd be a `PortalScrapeStatus.BOT_PROTECTED` státuszt.
  - Generálj közvetlen keresési linket a portálhoz a megadott feltételekkel (város, ár, típus).
  - Szükség esetén aktiváld a Python sidecart headless böngészővel (`playwright` vagy `curl_cffi`).

### 1.3 Hibás Ár- vagy Alapterület-értékek
* **Lehetséges okok:**
  - Az oldal pénznemformázása változott (pl. "54,9 M Ft", "54 900 000 Ft", vagy EUR árak).
  - Telekméret vs. lakóterület felcserélődése házak esetén.
* **Elhárítási lépések:**
  - Futtasd át az értéket a `PropertyParser.parsePrice(raw)` és `PropertyParser.parseFloorArea(raw)` metódusokon.
  - Szükség esetén egészítsd ki a regex mintákat a `PropertyParser`-ben új pénznemformátumokkal.

---

## 2. Portál Megbízhatósági Mátrix

| Portál | Elsődleges Módszer | Tartalék / Fallback | Fő Védelmi Mechanizmus |
| :--- | :--- | :--- | :--- |
| **Duna House** | Élő Jsoup DOM parsing | Közvetlen keresőlink | Nincs agresszív WAF, stabil HTML szerkezet |
| **Otthon Centrum** | Élő Jsoup DOM parsing | Közvetlen keresőlink | Rejtett sablonok szűrése, modern képattribútumok |
| **Jófogás** | Élő Jsoup DOM parsing | Közvetlen keresőlink | Query paraméterezés, ár regex tisztítás |
| **Ingatlanbazár** | Belső REST JSON API | Jsoup DOM parsing | Gyors JSON végpont, helykód gyorsítótárazás |
| **Költözzbe** | Élő DOM & JSON-LD | Közvetlen keresőlink | Dupla parsing (DOM kártyák + Schema.org) |
| **Ingatlan.com** | Graceful Link Fallback | Python Playwright Sidecar | Cloudflare WAF, Turnstile, JA3 ujjlenyomat |
