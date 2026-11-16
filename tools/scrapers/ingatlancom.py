"""
Ingatlan.com scraper – curl_cffi Chrome TLS impersonation segítségével.

A Cloudflare WAF bot-védelmet úgy kerüli meg, hogy a curl_cffi a Chrome böngésző
pontos TLS ujjlenyomatát (JA3/JA4) és HTTP/2 beállításait emulálja, így a szerver
nem különbözteti meg a kérést egy valódi böngészőétől.
"""

from __future__ import annotations

import re
import time
import unicodedata
from typing import Optional

from bs4 import BeautifulSoup, Tag
from curl_cffi import requests as curl_requests

from models import SidecarItem, SidecarResponse

BASE_URL = "https://ingatlan.com"
DEFAULT_MAX_PAGES = 20
DEFAULT_TIMEOUT_MS = 85000
LISTING_ID_URL_PATTERN = re.compile(r"/(?:ingatlan/)?(\d{6,12})(?:[/?#]|$)")
PAGE_QUERY_PATTERN = re.compile(r"(?:^|[?&])page=(\d+)(?:&|$)")


def _to_slug(text: str) -> str:
    """Ékezetmentesítés és kisbetűs slug konverzió."""
    if not text:
        return ""
    nfkd = unicodedata.normalize("NFKD", text)
    ascii_only = "".join(c for c in nfkd if not unicodedata.combining(c))
    return ascii_only.lower().strip().replace(" ", "-")


def _build_range_slug(min_val: Optional[int], max_val: Optional[int], unit: str) -> str:
    norm_min = min_val if min_val is not None and min_val > 0 else None
    norm_max = max_val if max_val is not None and 0 < max_val < 9999 else None
    if norm_min is not None and norm_max is not None:
        return f"{norm_min}-{norm_max}-{unit}"
    if norm_min is not None:
        return f"{norm_min}-{unit}-tol"
    if norm_max is not None:
        return f"{norm_max}-{unit}-ig"
    return ""


def _build_search_url(
    city: str,
    property_type: str = "lakas",
    min_price: Optional[int] = None,
    max_price: Optional[int] = None,
    min_area: Optional[int] = None,
    max_area: Optional[int] = None,
    page: int = 1,
) -> str:
    """Az IngatlanComUrlBuilder.buildSearchUrl() Java logikájának Python megfelelője."""
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

    segments = ["elado", type_slug]

    area_slug = _build_range_slug(min_area, max_area, "m2")
    if area_slug:
        segments.append(area_slug)

    price_slug = _build_range_slug(min_price, max_price, "mFt")
    if price_slug:
        segments.append(price_slug)

    segments.append(city_slug)
    base_url = f"{BASE_URL}/lista/{'+'.join(segments)}"
    if page > 1:
        separator = "&" if "?" in base_url else "?"
        return f"{base_url}{separator}page={page}"
    return base_url


def _parse_price_million(text: str) -> tuple[Optional[float], str]:
    """Kinyeri az árat M Ft egységben a kártya szövegéből."""
    if not text:
        return None, ""

    normalized = text.replace("\xa0", " ")
    m = re.search(r"(\d[\d\s,.]*)\s*M\s*Ft", normalized, re.IGNORECASE)
    if m:
        raw = m.group(0).strip()
        num_str = m.group(1).replace(" ", "").replace(",", ".")
        try:
            return float(num_str), raw
        except ValueError:
            return None, raw

    m = re.search(r"(\d[\d\s]*)\s*Ft(?!\s*/)", normalized, re.IGNORECASE)
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
    if not text:
        return None
    normalized = text.replace("\xa0", " ")
    m = re.search(r"Alapterület\s*(\d[\d,.\s]*)\s*m[²2]?", normalized, re.IGNORECASE)
    if not m:
        m = re.search(r"(?<!/\s)(\d[\d,.\s]*)\s*m[²2]", normalized, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1).replace(" ", "").replace(",", "."))
        except ValueError:
            pass
    return None


def _parse_rooms(text: str) -> Optional[float]:
    """Kinyeri a szobaszámot."""
    if not text:
        return None
    normalized = text.replace("\xa0", " ")
    m = re.search(r"Szobák\s*(\d+)\s*\+\s*(\d+)", normalized, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1)) + 0.5 * float(m.group(2))
        except ValueError:
            pass

    m = re.search(r"Szobák\s*(\d+(?:[.,]\d+)?)", normalized, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1).replace(",", "."))
        except ValueError:
            pass

    m = re.search(r"(\d+)\s*\+\s*(\d+)\s*(?:fél\s*)?szob", normalized, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1)) + 0.5 * float(m.group(2))
        except ValueError:
            pass

    m = re.search(r"(\d+(?:[.,]\d+)?)\s*szob", normalized, re.IGNORECASE)
    if m:
        try:
            return float(m.group(1).replace(",", "."))
        except ValueError:
            pass
    return None


def _extract_card_fields(card: Tag, city: str, item_id: str) -> tuple[str, Optional[float], str, Optional[float], Optional[float], Optional[str]]:
    card_text = card.get_text(" ", strip=True).replace("\xa0", " ")

    price_el = card.select_one(".fw-bold.fs-5, .price, .listing-card__price")
    price_source = price_el.get_text(" ", strip=True) if price_el else card_text
    price_million, price_raw = _parse_price_million(price_source)
    if price_million is None:
        price_million, price_raw = _parse_price_million(card_text)

    addr_el = card.select_one("span.d-block.fs-7.text-gray-900, .address, .listing-card__title")
    address_text = addr_el.get_text(" ", strip=True) if addr_el else ""

    is_new_label = card.select_one("[data-slug='uj-epitesu'], .listing-label")
    new_prefix = ""
    if is_new_label and "új építésű" in is_new_label.get_text(" ", strip=True).lower():
        new_prefix = " [Új építésű]"

    if address_text:
        title = f"{address_text}{new_prefix}"
    elif len(card_text) > 5:
        title = card_text[:65].strip() + "..."
    else:
        title = f"{city} - Ingatlan ({item_id})"

    area = _parse_area(card_text)
    rooms = _parse_rooms(card_text)

    img_el = card.select_one("img.listing-card-image, img[src*='ingatlancdn.com/resize']")
    image_url = None
    if img_el:
        src = img_el.get("src") or img_el.get("data-src")
        if isinstance(src, str) and src.startswith("http") and "listing-image-error" not in src:
            image_url = src

    return title, price_million, price_raw, area, rooms, image_url


def _has_pagination_without_next_page(
    soup: BeautifulSoup, current_page: int, added_count: int
) -> bool:
    has_any_page_link = False
    has_next_page = False
    next_page = current_page + 1

    for anchor in soup.select("a[href]"):
        href = anchor.get("href")
        if not isinstance(href, str):
            continue
        match = PAGE_QUERY_PATTERN.search(href)
        if not match:
            continue
        has_any_page_link = True
        if int(match.group(1)) == next_page:
            has_next_page = True
            break

    if has_any_page_link:
        return not has_next_page
    return current_page == 1 and added_count < 20


def _parse_page_cards_with_soup(
    html: str, city: str, seen_urls: set[str]
) -> tuple[list[SidecarItem], int, BeautifulSoup]:
    soup = BeautifulSoup(html, "html.parser")
    page_items: list[SidecarItem] = []

    cards = soup.select("a.listing-card, a[data-listing-id], div.listing-card, a[href*='/ingatlan/']")
    raw_card_count = len(cards)
    for card in cards:
        href = card.get("href", "") if card.name == "a" else ""
        if not href:
            inner_a = card.select_one("a[href]")
            href = inner_a.get("href", "") if inner_a else ""
        if not isinstance(href, str) or not href.strip():
            continue

        full_url = href if href.startswith("http") else BASE_URL + ("" if href.startswith("/") else "/") + href
        data_id = card.get("data-listing-id") or card.get("data-id")
        item_id = str(data_id).strip() if data_id else ""
        if not item_id:
            id_match = LISTING_ID_URL_PATTERN.search(full_url)
            if id_match:
                item_id = id_match.group(1)

        if not item_id or full_url in seen_urls:
            continue
        seen_urls.add(full_url)

        title, price_million, price_raw, area, rooms, image_url = _extract_card_fields(card, city, item_id)
        page_items.append(
            SidecarItem(
                id=item_id,
                url=full_url,
                title=title,
                priceMillion=price_million,
                priceRaw=price_raw,
                areaSquareMeters=area,
                roomCount=rooms,
                imageUrl=image_url,
            )
        )

    return page_items, raw_card_count, soup


def _parse_page_cards(html: str, city: str, seen_urls: set[str]) -> list[SidecarItem]:
    page_items, _, _ = _parse_page_cards_with_soup(html, city, seen_urls)
    return page_items


def scrape(
    city: str,
    property_type: str = "lakas",
    min_price: Optional[int] = None,
    max_price: Optional[int] = None,
    min_area: Optional[int] = None,
    max_area: Optional[int] = None,
    max_pages: int = DEFAULT_MAX_PAGES,
    timeout_ms: int = DEFAULT_TIMEOUT_MS,
    delay_ms: int = 200,
    session_factory=None,
) -> SidecarResponse:
    """
    Letölti az Ingatlan.com találati oldalait curl_cffi Chrome impersonation-nel,
    és visszaadja a strukturált SidecarResponse-t.
    """
    start_ms = int(time.time() * 1000)
    items: list[SidecarItem] = []
    seen_urls: set[str] = set()
    pages_to_fetch = max(1, min(max_pages, DEFAULT_MAX_PAGES))
    effective_timeout_ms = max(5000, timeout_ms)
    pages_processed = 0
    stop_reason = "MAX_PAGES_REACHED"

    try:
        session = (
            session_factory()
            if session_factory is not None
            else curl_requests.Session(impersonate="chrome")
        )
        for page in range(1, pages_to_fetch + 1):
            if page > 1 and (int(time.time() * 1000) - start_ms) >= effective_timeout_ms:
                stop_reason = "TIME_BUDGET_REACHED"
                break

            if page > 1 and delay_ms > 0:
                time.sleep(delay_ms / 1000.0)

            search_url = _build_search_url(
                city, property_type, min_price, max_price, min_area, max_area, page=page
            )
            referer = (
                BASE_URL + "/"
                if page == 1
                else _build_search_url(
                    city, property_type, min_price, max_price, min_area, max_area, page=page - 1
                )
            )
            resp = session.get(
                search_url,
                headers={
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Accept-Language": "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Referer": referer,
                },
                timeout=12,
            )

            if resp.status_code in (403, 429):
                if page == 1:
                    elapsed = int(time.time() * 1000) - start_ms
                    return SidecarResponse(
                        portalId="ingatlancom",
                        status="FAILED",
                        items=[],
                        errorMessage=f"Cloudflare HTTP {resp.status_code} – TLS impersonation blocked",
                        elapsedMs=elapsed,
                        pagesProcessed=0,
                        stopReason="FIRST_PAGE_BOT_PROTECTED",
                    )
                stop_reason = (
                    "INTERMEDIATE_PAGE_RATE_LIMITED"
                    if resp.status_code == 429
                    else "INTERMEDIATE_PAGE_BOT_PROTECTED"
                )
                break

            if resp.status_code != 200:
                if page == 1:
                    elapsed = int(time.time() * 1000) - start_ms
                    return SidecarResponse(
                        portalId="ingatlancom",
                        status="FAILED",
                        items=[],
                        errorMessage=f"HTTP {resp.status_code} hiba",
                        elapsedMs=elapsed,
                        pagesProcessed=0,
                        stopReason="FIRST_PAGE_HTTP_ERROR",
                    )
                stop_reason = f"INTERMEDIATE_PAGE_HTTP_{resp.status_code}"
                break

            pages_processed += 1
            page_items, raw_card_count, soup = _parse_page_cards_with_soup(
                resp.text, city, seen_urls
            )
            if raw_card_count == 0:
                stop_reason = "FIRST_PAGE_EMPTY" if page == 1 else "EMPTY_PAGE"
                break
            if not page_items:
                stop_reason = "REPEATED_PAGE_CONTENT"
                break

            items.extend(page_items)
            if page < pages_to_fetch and _has_pagination_without_next_page(
                soup, page, len(page_items)
            ):
                stop_reason = "NO_MORE_PAGES"
                break

        elapsed = int(time.time() * 1000) - start_ms
        return SidecarResponse(
            portalId="ingatlancom",
            status="SUCCESS",
            items=items,
            errorMessage=None,
            elapsedMs=elapsed,
            pagesProcessed=pages_processed,
            stopReason=stop_reason,
        )

    except Exception as e:
        elapsed = int(time.time() * 1000) - start_ms
        if items:
            return SidecarResponse(
                portalId="ingatlancom",
                status="SUCCESS",
                items=items,
                errorMessage=str(e),
                elapsedMs=elapsed,
                pagesProcessed=pages_processed,
                stopReason="INTERMEDIATE_PAGE_EXCEPTION",
            )
        return SidecarResponse(
            portalId="ingatlancom",
            status="FAILED",
            items=[],
            errorMessage=str(e),
            elapsedMs=elapsed,
            pagesProcessed=pages_processed,
            stopReason="EXCEPTION",
        )
