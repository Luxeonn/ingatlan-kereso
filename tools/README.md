# Scraper Sidecar – Node.js Playwright CLI

Az Ingatlan.com Cloudflare-védett hirdetéseinek kinyerésére szolgáló on-demand CLI segédmodul (`tools/sidecar-playwright.mjs`), amelyet a Java backend (`SidecarCliExecutor` → `IngatlanComSidecarStrategy`) hív meg.

## Futtatókörnyezet és telepítés

```bash
cd tools && npm ci
```

Headless Chromium (`--headless=new`) futtatással kerüli meg a TLS/JS korlátozásokat (`playwright-core`); egyedi böngésző útvonal a `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` környezeti változóval adható meg.

**Kézi böngészős fallback:** Ha a portál a Playwright sidecart is blokkolja, a felület közvetlen keresőlinket és kézi böngészős HTML-importot (`POST /api/portals/ingatlancom/import-html`) kínál fel.

## Használat és CLI paraméterek

```bash
node tools/sidecar-playwright.mjs --portal ingatlancom --city budapest --type haz --max-pages 5
```

| Paraméter | Kötelező | Alapértelmezett | Leírás |
| :--- | :--- | :--- | :--- |
| `--portal` | ✅ | `ingatlancom` | Portál azonosító (`ingatlancom`) |
| `--city` | ✅ | `debrecen` | Város neve (ékezetmentesítve is elfogadja) |
| `--type` | ❌ | `lakas` | `lakas` \| `haz` \| `telek` \| `garazs` \| `nyaralo` \| `iroda` \| `osszes` |
| `--min-price` / `--max-price` | ❌ | – | Árszűrő millió Ft-ban |
| `--min-area` / `--max-area` | ❌ | – | Alapterület m²-ben |
| `--max-pages` | ❌ | `20` | Maximálisan letöltendő oldalak száma |
| `--timeout-ms` | ❌ | `125000` | Puha időkeret ezredmásodpercben a részleges eredményhez |

## JSON kimeneti formátum (`stdout`)

```json
{
  "portalId": "ingatlancom",
  "status": "SUCCESS",
  "items": [
    {
      "id": "12345678",
      "url": "https://ingatlan.com/12345678",
      "title": "Debrecen - Belvárosi 2 szobás lakás",
      "priceMillion": 42.5,
      "priceRaw": "42,5 M Ft",
      "areaSquareMeters": 58.0,
      "roomCount": 2.0,
      "imageUrl": "https://ingatlan.com/images/..."
    }
  ],
  "errorMessage": null,
  "elapsedMs": 850,
  "pagesProcessed": 1,
  "stopReason": "LAST_PAGE"
}
```

## Könyvtárszerkezet

```text
tools/
├── sidecar-playwright.mjs           # Node.js + playwright-core Chromium sidecar
├── sidecar-playwright.test.mjs      # Node.js unit tesztek
├── package.json & package-lock.json # Node sidecar függőségek (playwright-core)
└── README.md                        # Ez az útmutató
```

## Java és Docker integráció

- **Java (`SidecarCliExecutor` → `IngatlanComSidecarStrategy`):** 3 perces válasz-gyorsítótárral és dinamikus időkerettel futtatja a `tools/sidecar-playwright.mjs` szkriptet; hiba vagy részleges bot-védelem esetén Jsoup/közvetlen keresőlink tartalékra áll át (0-crash garancia).
- **Docker (`Dockerfile`):** A backend image (`alpine:3.24` + egyedi `jlink` JRE 25) a `nodejs`, `chromium` és `playwright-core` (`tools/sidecar-playwright.mjs`, `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium-browser`, nem-root `appuser`) sidecart tartalmazza.
- **Tesztek futtatása:** `node --test tools/sidecar-playwright.test.mjs`
