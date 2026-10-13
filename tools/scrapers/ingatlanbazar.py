"""
Ingatlanbazár scraper – Placeholder modul.

Az Ingatlanbazár portál Angular SPA architektúrájú, azonban a Java oldali
IngatlanBazarScraper már sikeresen használja a portál nyílt belső REST API-ját
(GET /api/property-search), így jelenleg NINCS szükség Playwright sidecar-ra.

Ez a modul akkor aktiválódik, ha a jövőben az Ingatlanbazár API elérhetetlenné
válik és Playwright headless renderelésre lesz szükség.
"""

from __future__ import annotations

from typing import Optional

from models import SidecarResponse


def scrape(
    city: str,
    property_type: str = "lakas",
    min_price: Optional[int] = None,
    max_price: Optional[int] = None,
) -> SidecarResponse:
    """
    Placeholder: az Ingatlanbazár jelenleg a Java REST API scraperrel működik.
    Ha a jövőben Playwright szükséges, itt implementálandó.
    """
    return SidecarResponse(
        portalId="ingatlanbazar",
        status="FAILED",
        items=[],
        errorMessage="Ingatlanbazar sidecar not implemented – use Java REST API scraper instead",
        elapsedMs=0,
    )
