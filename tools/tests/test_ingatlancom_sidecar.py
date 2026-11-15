import os
import sys
import unittest

TOOLS_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if TOOLS_DIR not in sys.path:
    sys.path.insert(0, TOOLS_DIR)

from scrapers import ingatlancom


def _build_page_html(page_num: int, count: int, pagination_links: list[str]) -> str:
    start_idx = (page_num - 1) * 20 + 1
    cards_html = []
    for i in range(count):
        listing_id = 2000000 + start_idx + i
        cards_html.append(
            f'<a class="listing-card" data-listing-id="{listing_id}" href="/{listing_id}">'
            f'<span class="d-block fs-7 text-gray-900">Hajdúszoboszló ház {listing_id}</span>'
            f'<span class="fw-bold fs-5">85 M Ft</span>'
            f'<span>Alapterület 120 m² Szobák 4</span>'
            f'</a>'
        )
    nav_html = "".join(f'<a href="{href}">{href}</a>' for href in pagination_links)
    return f"<html><body>{''.join(cards_html)}{nav_html}</body></html>"


class _FakeResponse:
    def __init__(self, status_code: int, text: str = ""):
        self.status_code = status_code
        self.text = text


class _FakeSession:
    def __init__(self, handler):
        self._handler = handler
        self.requested_urls: list[str] = []

    def get(self, url: str, headers=None, timeout=None):
        self.requested_urls.append(url)
        return self._handler(url)


class IngatlanComSidecarTest(unittest.TestCase):
    def test_build_search_url_uses_page_query_without_sort_segments(self):
        base = "https://ingatlan.com/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo"
        self.assertEqual(
            ingatlancom._build_search_url("Hajdúszoboszló", "haz", 60, 130, 95, 200, page=1),
            base,
        )
        self.assertEqual(
            ingatlancom._build_search_url("Hajdúszoboszló", "haz", 60, 130, 95, 200, page=2),
            f"{base}?page=2",
        )
        self.assertEqual(
            ingatlancom._build_search_url("Hajdúszoboszló", "haz", 60, 130, 95, 200, page=6),
            f"{base}?page=6",
        )

    def test_scrape_collects_all_102_listings_across_6_pages(self):
        def handler(url: str) -> _FakeResponse:
            page = 1
            if "page=" in url:
                page = int(url.split("page=")[-1])
            if page < 6:
                return _FakeResponse(200, _build_page_html(page, 20, [f"/lista/x?page={page + 1}"]))
            if page == 6:
                return _FakeResponse(200, _build_page_html(6, 2, ["/lista/x?page=5"]))
            raise AssertionError(f"Unexpected page requested: {url}")

        fake_session = _FakeSession(handler)
        resp = ingatlancom.scrape(
            city="Hajdúszoboszló",
            property_type="haz",
            min_price=60,
            max_price=130,
            min_area=95,
            max_area=200,
            max_pages=20,
            delay_ms=0,
            session_factory=lambda: fake_session,
        )

        self.assertEqual(resp.status, "SUCCESS")
        self.assertEqual(len(resp.items), 102)
        self.assertEqual(resp.pages_processed, 6)
        self.assertEqual(resp.stop_reason, "NO_MORE_PAGES")
        self.assertEqual(len(fake_session.requested_urls), 6)

    def test_scrape_preserves_partial_results_on_intermediate_403(self):
        def handler(url: str) -> _FakeResponse:
            if "page=" not in url:
                return _FakeResponse(200, _build_page_html(1, 20, ["/lista/x?page=2"]))
            if "page=2" in url:
                return _FakeResponse(200, _build_page_html(2, 20, ["/lista/x?page=3"]))
            return _FakeResponse(403, "")

        fake_session = _FakeSession(handler)
        resp = ingatlancom.scrape(
            city="Hajdúszoboszló",
            property_type="haz",
            max_pages=20,
            delay_ms=0,
            session_factory=lambda: fake_session,
        )

        self.assertEqual(resp.status, "SUCCESS")
        self.assertEqual(len(resp.items), 40)
        self.assertEqual(resp.pages_processed, 2)
        self.assertEqual(resp.stop_reason, "INTERMEDIATE_PAGE_BOT_PROTECTED")

    def test_scrape_stops_on_repeated_page_content(self):
        fake_session = _FakeSession(
            lambda _url: _FakeResponse(200, _build_page_html(1, 20, ["/lista/x?page=2"]))
        )
        resp = ingatlancom.scrape(
            city="Debrecen",
            property_type="lakas",
            max_pages=20,
            delay_ms=0,
            session_factory=lambda: fake_session,
        )
        self.assertEqual(resp.status, "SUCCESS")
        self.assertEqual(len(resp.items), 20)
        self.assertEqual(resp.stop_reason, "REPEATED_PAGE_CONTENT")


if __name__ == "__main__":
    unittest.main()
