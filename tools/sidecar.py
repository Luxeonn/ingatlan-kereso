#!/usr/bin/env python3
"""
Scraper Sidecar – On-demand CLI segédmodul a bot-védett ingatlanportálokhoz.

Használat:
    python tools/sidecar.py --portal ingatlancom --city debrecen --type lakas --min-price 30 --max-price 50 --min-area 50 --max-area 120

A kimenet mindig JSON formátumban érkezik a stdout-ra.
A stderr kizárólag debug logoknak van fenntartva.
"""

from __future__ import annotations

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from models import SidecarResponse

SUPPORTED_PORTALS = ["ingatlancom"]
SUPPORTED_TYPES = ["lakas", "haz", "telek", "garazs", "nyaralo", "iroda", "osszes"]


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scraper Sidecar – Ingatlanportál scraping CLI",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--portal",
        required=True,
        choices=SUPPORTED_PORTALS,
        help="Cél portál azonosító (ingatlancom)",
    )
    parser.add_argument(
        "--city",
        required=True,
        help="Város neve (pl. debrecen, budapest, szeged)",
    )
    parser.add_argument(
        "--type",
        default="lakas",
        choices=SUPPORTED_TYPES,
        dest="property_type",
        help="Ingatlan típus (default: lakas)",
    )
    parser.add_argument(
        "--min-price",
        type=int,
        default=None,
        help="Minimum ár millió Ft-ban",
    )
    parser.add_argument(
        "--max-price",
        type=int,
        default=None,
        help="Maximum ár millió Ft-ban",
    )
    parser.add_argument(
        "--min-area",
        type=int,
        default=None,
        help="Minimum alapterület m2-ben",
    )
    parser.add_argument(
        "--max-area",
        type=int,
        default=None,
        help="Maximum alapterület m2-ben",
    )
    parser.add_argument(
        "--max-pages",
        type=int,
        default=1,
        help="Maximálisan letöltendő oldalak száma (default: 1)",
    )
    return parser.parse_args()


def main() -> None:
    args = _parse_args()

    response: SidecarResponse

    try:
        from scrapers.ingatlancom import scrape

        response = scrape(
            city=args.city,
            property_type=args.property_type,
            min_price=args.min_price,
            max_price=args.max_price,
            min_area=args.min_area,
            max_area=args.max_area,
            max_pages=args.max_pages,
        )
    except ImportError as e:
        response = SidecarResponse(
            portalId=args.portal,
            status="FAILED",
            errorMessage=f"Missing dependency: {e}. Run: pip install -r tools/requirements.txt",
        )
    except Exception as e:
        response = SidecarResponse(
            portalId=args.portal,
            status="FAILED",
            errorMessage=f"Unexpected error: {e}",
        )

    _output(response)


def _output(response: SidecarResponse) -> None:
    """JSON kimenetet ír a stdout-ra camelCase mezőnevekkel."""
    print(json.dumps(response.model_dump(by_alias=True), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
