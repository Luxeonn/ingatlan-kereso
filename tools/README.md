# Scraper Sidecar – Python CLI Segédmodul

On-demand CLI segédmodul a Java alkalmazás számára, amely az Ingatlan.com hirdetéseit próbálja kinyerni.

## Telepítés (Fejlesztői környezetben)

```bash
# Python 3.10+ szükséges
pip install -r tools/requirements.txt
```

> **Megjegyzés:** Végfelhasználóknak NEM kell Python-t telepíteni – a `tools/scraper-sidecar.exe` önálló bináris kerül kiadásra (lásd Fázis 3).

## Használat

### Alap keresés
```bash
python tools/sidecar.py --portal ingatlancom --city debrecen --type lakas
```

### Árszűréssel
```bash
python tools/sidecar.py --portal ingatlancom --city budapest --type lakas --min-price 30 --max-price 60
```

### Kimenet
A kimenet mindig **JSON** formátumban érkezik a `stdout`-ra:

```json
{
  "portalId": "ingatlancom",
  "status": "SUCCESS",
  "items": [
    {
      "id": "12345678",
      "url": "https://ingatlan.com/12345678",
      "title": "Debrecen - Belvárosi 2 szobás lakás...",
      "priceMillion": 42.5,
      "priceRaw": "42,5 M Ft",
      "areaSquareMeters": 58.0,
      "roomCount": 2.0
    }
  ],
  "errorMessage": null,
  "elapsedMs": 850
}
```

## CLI Paraméterek

| Paraméter | Kötelező | Alapértelmezett | Leírás |
|:---|:---|:---|:---|
| `--portal` | ✅ | – | Portál azonosító: `ingatlancom` |
| `--city` | ✅ | – | Város neve (ékezetmentesítve is elfogadja) |
| `--type` | ❌ | `lakas` | `lakas` \| `haz` \| `telek` \| `garazs` \| `nyaralo` \| `iroda` \| `osszes` |
| `--min-price` | ❌ | – | Minimum ár millió Ft-ban |
| `--max-price` | ❌ | – | Maximum ár millió Ft-ban |

## Architektúra

```
tools/
├── sidecar.py              # Fő CLI belépési pont (argparse → router)
├── models.py               # Pydantic JSON kimeneti séma
├── scrapers/
│   └── ingatlancom.py      # curl_cffi Chrome TLS impersonation
├── requirements.txt        # Python függőségek
└── README.md               # Ez a fájl
```

## Hogyan hívja a Java?

A `SidecarCliExecutor` (Fázis 2) a `ProcessBuilder` segítségével futtatja:

```java
ProcessBuilder pb = new ProcessBuilder(
    "tools/scraper-sidecar.exe",  // vagy: "python", "tools/sidecar.py"
    "--portal", "ingatlancom",
    "--city", citySlug,
    "--type", propertyType,
    "--min-price", String.valueOf(minPrice),
    "--max-price", String.valueOf(maxPrice)
);
```

Ha a sidecar nem elérhető vagy hibát dob, a Java oldal automatikusan a meglévő Jsoup/fallback útvonalra vált (0-crash garancia).
