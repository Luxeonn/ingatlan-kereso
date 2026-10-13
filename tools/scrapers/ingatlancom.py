"""
Ingatlan.com scraper – curl_cffi Chrome TLS impersonation segítségével.

A Cloudflare WAF bot-védelmet úgy kerüli meg, hogy a curl_cffi a Chrome böngésző
pontos TLS ujjlenyomatát (JA3/JA4) és HTTP/2 beállításait emulálja, így a szerver
nem különbözteti meg a kérést egy valódi böngészőétől.

Nincs szükség headless böngészőre: a válaszidő < 1 mp, memóriahasználat < 30 MB.
"""

from __future__ import annotations

import re
import time
import unicodedata
from typing import Optional

from curl_cffi import requests as curl_requests
from bs4 import BeautifulSoup

from models import SidecarItem, SidecarResponse


# Ingatlan.com állandók
BASE_URL = "https://ingatlan.com"
ID_PATTERN = re.compile(r"/ingatlan/(\d+)")

# Ékezet eltávolító a URL slug képzéséhez (a Java SlugUtils.toSlug() tükrözése)
def _to_slug(text: str) -> str:
    """Ékezetmentesítés és kisbetűs slug konverzió."""
    if not text:
        return ""
    # NFD normalizálás: szétbontja az ékezetes karaktereket alap + módosítóra
    nfkd = unicodedata.normalize("NFKD", text)
    ascii_only = "".join(c for c in nfkd if not unicodedata.combining(c))
    return ascii_only.lower().strip().replace(" ", "-")


def _build_search_url(
    city: str,
    property_type: str = "lakas",
    min_price: Optional[int] = None,
    max_price: Optional[int] = None,
) -> str:
    """Az IngatlanComScraper.buildSearchUrl() Java logikájának Python megfelelője."""
    city_slug = _to_slug(city) if city else "debrecen"
    if not city_slug:
        city_slug = "debrecen"

    type_map = {
        "haz": "haz",
        "garazs": "garazs",
        "telek": "telek",
        "nyaralo": "nyaralo",
        "iroda": "iroda",
        "osszes": "ingatlan",
    }
    type_slug = type_map.get(property_type.lower().strip(), "lakas") if property_type else "lakas"

    price_slug = ""
    if min_price is not None and max_price is not None:
        price_slug = f"+{min_price}-{max_price}-mFt"
    elif min_price is not None:
        price_slug = f"+{min_price}-mFt-tol"
    elif max_price is not None:
        price_slug = f"+{max_price}-mFt-ig"

    return f"{BASE_URL}/lista/elado+{type_slug}+{city_slug}{price_slug}"


def _parse_price_million(text: str) -> tuple[Optional[float], str]:
    """
    Kinyeri az árat M Ft egységben a kártya szövegéből.
    Visszaadja a (millió Ft, nyers szöveg) párost.
    """
    if not text:
        return None, ""

    # "42,5 M Ft" vagy "42.5 M Ft" mintázat
    m = re.search(r"(\d[\d\s,.]*)\s*M\s*Ft", text, re.IGNORECASE)
    if m:
        raw = m.group(0).strip()
        num_str = m.group(1).replace(" ", "").replace(",", ".")
        try:
            return float(num_str), raw
        except ValueError:
            return None, raw

    # "42 500 000 Ft" vagy "42500000 Ft" mintázat
    m = re.search(r"(\d[\d\s]*)\s*Ft", text, re.IGNORECASE)
    if m:
        raw = m.group(0).strip()
        num_str = m.group(1).replace(" ", "")
        try:
            value_ft = int(num_str)
            return round(value_ft / 1_000_000, 2), raw
        except ValueError:
            return None, raw

    return None, ""


def _parse_area(text: str) -> Optional[float]:
    """Kinyeri az alapterületet m²-ben."""
    m = re.search(r"(\d[\d,.\s]*)\s*m[²2]", text, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1).replace(" ", "").replace(",", "."))
        except ValueError:
            pass
    return None


def _parse_rooms(text: str) -> Optional[float]:
    """Kinyeri a szobaszámot."""
    m = re.search(r"(\d+(?:[.,]\d+)?)\s*szob", text, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1).replace(",", "."))
        except ValueError:
            pass
    # "2+1 szoba" formátum
    m = re.search(r"(\d+)\s*\+\s*(\d+)\s*(?:fél\s*)?szob", text, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1)) + 0.5 * float(m.group(2))
        except ValueError:
            pass
    return None


def scrape(
    city: str,
    property_type: str = "lakas",
    min_price: Optional[int] = None,
    max_price: Optional[int] = None,
) -> SidecarResponse:
    """
    Letölti az Ingatlan.com találati oldalát curl_cffi Chrome impersonation-nel,
    és visszaadja a strukturált SidecarResponse-t.
    """
    start_ms = int(time.time() * 1000)
    search_url = _build_search_url(city, property_type, min_price, max_price)

    try:
        # Chrome 120 TLS ujjlenyomat emuláció
        session = curl_requests.Session(impersonate="chrome120")
        resp = session.get(
            search_url,
            headers={
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Accept-Language": "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7",
                "Referer": BASE_URL + "/",
            },
            timeout=10,
        )

        if resp.status_code == 403:
            elapsed = int(time.time() * 1000) - start_ms
            return SidecarResponse(
                portalId="ingatlancom",
                status="FAILED",
                items=[],
                errorMessage=f"Cloudflare 403 Forbidden – TLS impersonation blocked (HTTP {resp.status_code})",
                elapsedMs=elapsed,
            )

        if resp.status_code != 200:
            elapsed = int(time.time() * 1000) - start_ms
            return SidecarResponse(
                portalId="ingatlancom",
                status="FAILED",
                items=[],
                errorMessage=f"HTTP {resp.status_code} hiba",
                elapsedMs=elapsed,
            )

        soup = BeautifulSoup(resp.text, "html.parser")
        items: list[SidecarItem] = []
        seen_urls: set[str] = set()

        # Hirdetéskártyák keresése – az Ingatlan.com a hirdetéseket /ingatlan/<id> linkekkel tartalmazza
        for a_tag in soup.select("a[href*='/ingatlan/']"):
            href = a_tag.get("href", "")
            if not href:
                continue

            full_url = href if href.startswith("http") else BASE_URL + ("" if href.startswith("/") else "/") + href

            # Csak valódi hirdetéslinkek (/ingatlan/<számok>)
            if not re.search(r"/ingatlan/\d+", full_url):
                continue
            if full_url in seen_urls:
                continue
            seen_urls.add(full_url)

            card_text = a_tag.get_text(" ", strip=True)

            # ID kinyerés
            id_match = ID_PATTERN.search(full_url)
            item_id = id_match.group(1) if id_match else ""

            # Adatok kinyerése a kártya szövegéből
            price_million, price_raw = _parse_price_million(card_text)
            area = _parse_area(card_text)
            rooms = _parse_rooms(card_text)

            # Cím konstruálás
            title = card_text[:65].strip() + "..." if len(card_text) > 5 else f"{city} - Ingatlan ({item_id})"

            items.append(
                SidecarItem(
                    id=item_id,
                    url=full_url,
                    title=f"{city} - {title}",
                    priceMillion=price_million,
                    priceRaw=price_raw,
                    areaSquareMeters=area,
                    roomCount=rooms,
                )
            )

        elapsed = int(time.time() * 1000) - start_ms

        if not items:
            return SidecarResponse(
                portalId="ingatlancom",
                status="SUCCESS",
                items=[],
                errorMessage=None,
                elapsedMs=elapsed,
            )

        return SidecarResponse(
            portalId="ingatlancom",
            status="SUCCESS",
            items=items,
            errorMessage=None,
            elapsedMs=elapsed,
        )

    except Exception as e:
        elapsed = int(time.time() * 1000) - start_ms
        return SidecarResponse(
            portalId="ingatlancom",
            status="FAILED",
            items=[],
            errorMessage=str(e),
            elapsedMs=elapsed,
        )
