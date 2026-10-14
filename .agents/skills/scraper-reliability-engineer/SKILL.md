---
name: scraper-reliability-engineer
description: >-
  Web scraping robusztussági és adatkinyerési megbízhatósági mérnök. Használd ezt a skillt portál scraper
  motorok fejlesztéséhez és hibaelhárításához, Cloudflare és WAF bot-védelem detektálásához és kezeléséhez,
  HTTP ujjlenyomat és fejlécek beállításához, DOM struktúraváltozások felderítéséhez, JSON-LD és belső REST API-k
  feldolgozásához, valamint a 0-crash graceful degradation garantálásához.
metadata:
  icon: find_in_page
---

# Scraper Megbízhatósági Mérnök Skill (Scraper Reliability & Anti-Bot Engineer)

Ez a skill a web scraping robusztusságának, a külső portálok DOM- és API-változásainak, az anti-bot védelmi mechanizmusok felismerésének és a hibatűrő adatkinyerésnek a szabályzatát rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Alapelvek és Filozófia

A web scraping eredendően törékeny ökoszisztéma: a weboldalak szerkezete, védelmi falai és URL sémái bármikor megváltozhatnak.

### 1.1 A 0-Crash Aranyszabály
* **Izolált Hibatűrés:** Egyetlen külső weboldal hibája, időtúllépése, 403/503-as válasza vagy megváltozott HTML fája sem okozhatja az alkalmazás összeomlását vagy a többi portál keresésének megszakadását.
* **Graceful Degradation:** Ha a közvetlen letöltés vagy feldolgozás nem lehetséges, a scraper azonnal és elegánsan térjen vissza közvetlen hirdetési keresőlinkkel (`PortalScrapeStatus.BOT_PROTECTED` vagy `ERROR`), és részletes naplóüzenettel tájékoztassa a felhasználót.

---

## 2. Anti-Bot és WAF Detektálási Minták

A magyar ingatlanportálok jelentős része (különösen az Ingatlan.com) aktív Cloudflare WAF vagy más bot-védelmi rendszert használ.

### 2.1 Detektálási Jelek (Indicators of Compromise / Bot Challenge)
1. **HTTP Státuszkódok:** HTTP 403 Forbidden vagy HTTP 503 Service Unavailable.
2. **Jellegzetes Fejlécek:** `cf-mitigated: challenge`, `cf-ray: ...`, `server: cloudflare`.
3. **Oldaltartalmi Minták:**
   - "Checking your browser before accessing..."
   - "Just a moment..."
   - "Verify you are human" / Turnstile captcha elemek
   - Cloudflare Ray ID szövegek a HTML-ben.

```java
// Példa beépített Cloudflare detektálásra
public static boolean isBotProtected(int statusCode, String htmlBody) {
    if (statusCode == 403 || statusCode == 503) {
        return true;
    }
    if (htmlBody == null) {
        return false;
    }
    String lower = htmlBody.toLowerCase(Locale.ROOT);
    return lower.contains("just a moment")
        || lower.contains("checking your browser")
        || lower.contains("cf-mitigated")
        || lower.contains("challenge-running");
}
```

---

## 3. Adatkinyerési Hierarchia (Resilience Hierarchy)

Új portál támogatásakor vagy meglévő frissítésekor az alábbi megbízhatósági sorrendet kell követni:

1. **Belső REST JSON API (Legstabilabb):**
   - Ha az oldal rendelkezik belső, publikusan elérhető JSON végponttal (pl. Ingatlanbazár `/api/property-search`), mindig ezt preferáld a HTML parsinggal szemben.
   - Nincs DOM változási kockázat, strukturált mezők, gyors válaszidő.
2. **Schema.org JSON-LD / Microdata (Kifejezetten stabil):**
   - Sok modern portál (pl. Költözzbe) beágyazza az adatokat `<script type="application/ld+json">` tagekbe.
   - A strukturált JSON közvetlenül deszerializálható, függetlenül a CSS osztályok változásától.
3. **Élő Jsoup DOM Parsing Rezisztens Szelektorokkal:**
   - Kerüld a mélyen egymásba ágyazott, instabil CSS szelektorokat (pl. `div > div:nth-child(2) > span`).
   - Használj szemantikai adatattribútumokat (`[data-property-id]`, `[data-price]`) vagy stabil osztályrészleteket.
4. **Hibrid Sidecar / Headless Böngésző (Végső megoldás):**
   - Erős JavaScript renderelés vagy TLS ujjlenyomat-ellenőrzés esetén külső Python / Playwright CLI sidecar hívása stdout IPC-n keresztül.

---

## 4. HTTP Ujjlenyomat és Fejléc Hardening (`HttpFetcher.java`)

A scraping kéréseknek maximálisan egy modern asztali böngésző hálózati forgalmát kell szimulálniuk:
* **User-Agent:** Mindig aktuális, érvényes Chrome/Firefox User-Agent string.
* **Client Hints Fejlécek:**
  - `Sec-Ch-Ua: "Chromium";v="133", "Google Chrome";v="133", "Not?A_Brand";v="99"`
  - `Sec-Ch-Ua-Mobile: ?0`
  - `Sec-Ch-Ua-Platform: "Windows"`
* **Fetch Metadata Fejlécek:**
  - `Sec-Fetch-Dest: document`
  - `Sec-Fetch-Mode: navigate`
  - `Sec-Fetch-Site: same-origin`
  - `Sec-Fetch-User: ?1`
* **Időtúllépés:** Szigorú 15 másodperces hálózati timeout.

A részletes hibaelhárítási forgatókönyveket a [Scraper Megbízhatósági Forgatókönyvben](./references/scraper-reliability-playbook.md) találod.

---

## 5. Scraper Megbízhatósági Ellenőrző Lista

- [ ] **0-Crash garancia:** Hálózati vagy parsing kivétel esetén a scraper visszatér `BOT_PROTECTED` vagy `ERROR` státusszal összeomlás helyett?
- [ ] **Szelektor stabilitás:** A használt szelektorok nem támaszkodnak véletlenszerűen generált CSS osztályokra?
- [ ] **Modern fejlécek:** A kérések tartalmazzák az érvényes User-Agent és Client Hints fejléceket?
- [ ] **JSON-LD vizsgálat:** Megvizsgáltad, hogy elérhető-e Schema.org JSON-LD vagy belső REST API?
- [ ] **Ár és méret normalizálás:** A kinyert adatok a `PropertyParser` segédosztályon mennek keresztül és helyesen tisztulnak?
- [ ] **Offline tesztlefedettség:** Készült mentett HTML fixture-re épülő egységteszt a scraperhez?
