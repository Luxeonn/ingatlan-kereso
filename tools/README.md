# Scraper Sidecar – Python & Playwright CLI

Az Ingatlan.com Cloudflare-védett hirdetéseinek kinyerésére szolgáló on-demand CLI segédmodulok, amelyeket a Java backend automatikus tartaléklánccal hív meg.

## Futtatókörnyezetek és telepítés

1. **Elsődleges sidecar (`tools/sidecar.py` — Python 3.10+ + `curl_cffi`):**

   ```bash
   pip install -r tools/requirements.txt
   ```

   Függőségek (`tools/requirements.txt`): `curl_cffi`, `beautifulsoup4` (a válaszmodellek natív Python `dataclass`-t használnak).

2. **Tartalék sidecar (`tools/sidecar-playwright.mjs` — Node.js + `playwright-core`):**

   ```bash
   cd tools && npm ci
   ```

   Headless Chromium (`--headless=new`) futtatással kerüli meg a TLS/JS korlátozásokat; egyedi böngésző útvonal a `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` környezeti változóval adható meg.

3. **Kézi böngészős fallback:** Ha a portál mindkét sidecart blokkolja, a felület közvetlen keresőlinket és kézi böngészős HTML-importot (`POST /api/portals/ingatlancom/import-html`) kínál fel.

## Használat és CLI paraméterek

```bash
# Python sidecar
python tools/sidecar.py --portal ingatlancom --city debrecen --type lakas --min-price 30 --max-price 60

# Playwright Chromium sidecar
node tools/sidecar-playwright.mjs --portal ingatlancom --city budapest --type haz --max-pages 5
```

| Paraméter | Kötelező | Alapértelmezett | Leírás |
| :--- | :--- | :--- | :--- |
| `--portal` | ✅ | `ingatlancom` (`sidecar.py`-nál kötelező) | Portál azonosító (`ingatlancom`) |
| `--city` | ✅ | `debrecen` (`sidecar-playwright.mjs` alap) | Város neve (ékezetmentesítve is elfogadja) |
| `--type` | ❌ | `lakas` | `lakas` \| `haz` \| `telek` \| `garazs` \| `nyaralo` \| `iroda` \| `osszes` |
| `--min-price` / `--max-price` | ❌ | – | Árszűrő millió Ft-ban |
| `--min-area` / `--max-area` | ❌ | – | Alapterület m²-ben |
| `--max-pages` | ❌ | `20` | Maximálisan letöltendő oldalak száma |
| `--timeout-ms` | ❌ | `85000` (Python) / `125000` (Playwright) | Puha időkeret ezredmásodpercben a részleges eredményhez |

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
├── sidecar.py                       # Python CLI belépési pont
├── models.py                        # Stdlib dataclass JSON válaszmodellek
├── scrapers/
│   └── ingatlancom.py               # curl_cffi alapú Ingatlan.com kinyerő
├── sidecar-playwright.mjs           # Node.js + playwright-core Chromium tartalék sidecar
├── package.json & package-lock.json # Node sidecar függőségek (playwright-core)
├── requirements.txt                 # Python függőségek
├── tests/
│   └── test_ingatlancom_sidecar.py  # Python unit tesztek
├── sidecar-playwright.test.mjs      # Node.js unit tesztek
└── README.md                        # Ez az útmutató
```

## Java és Docker integráció

- **Java (`SidecarCliExecutor` → `IngatlanComSidecarStrategy`):** 3 perces válasz-gyorsítótárral és dinamikus időkerettel futtatja a `tools/sidecar.py` szkriptet; hiba vagy részleges bot-védelem (`INTERMEDIATE_PAGE_BOT_PROTECTED` / `INTERMEDIATE_PAGE_RATE_LIMITED`) esetén automatikusan továbbáll a `tools/sidecar-playwright.mjs` futtatóra, végső esetben pedig Jsoup/közvetlen keresőlink tartalékra (0-crash garancia).
- **Docker (`Dockerfile`):** A backend image (`alpine:3.24` + egyedi `jlink` JRE 25) alapértelmezetten a könnyűsúlyú Python `curl_cffi` sidecart (`python3`, `tools/requirements.txt`, nem-root `appuser`) tartalmazza Chromium és Node.js nélkül; a `tools/sidecar-playwright.mjs` lokális fejlesztői környezetben használható tartalékként.
- **Tesztek futtatása:** `python -m unittest discover -s tools/tests` és `node --test tools/sidecar-playwright.test.mjs`.
