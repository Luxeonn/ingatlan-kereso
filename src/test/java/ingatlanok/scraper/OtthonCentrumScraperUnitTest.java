package ingatlanok.scraper;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtthonCentrumScraperUnitTest {

    @Test
    @DisplayName("OtthonCentrumScraper URL builder generates valid query parameters")
    void testBuildSearchUrl() {
        OtthonCentrumScraper scraper = new OtthonCentrumScraper();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(50);
        criteria.setMaxPriceMillion(120);

        String url = scraper.buildSearchUrl(criteria);
        assertEquals("https://www.oc.hu/elado/haz/debrecen", url);

        SearchCriteria budapestCriteria = new SearchCriteria();
        budapestCriteria.setCity("Budapest");
        budapestCriteria.setPropertyType("lakas");
        String bpUrl = scraper.buildSearchUrl(budapestCriteria);
        assertEquals("https://www.oc.hu/elado/lakas/budapest", bpUrl);
    }

    @Test
    @DisplayName("OtthonCentrumScraper parses cards with data-seo-select-item-value JSON and HTML")
    void testScrapeWithSeoJsonAndCards() {
        String sampleHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="real-estate-card" data-seo-select-item-value='{"ecommerce":{"items":[{"location_city":"Debrecen","price":88000000}]}}'>
                    <a href="/ingatlanok/H987654" class="card-link">
                        <img src="https://oc.hu/img/test.jpg" alt="Családi ház">
                        <div class="card-body">
                            <h3>Debrecen - Eladó családi ház</h3>
                            <span class="price">88 M Ft</span>
                            <span class="details">95 m² 3 szoba</span>
                        </div>
                    </a>
                </div>
            </body>
            </html>
            """;

        OtthonCentrumScraper scraper = new OtthonCentrumScraper() {
            @Override
            public Document getDocument(String targetUrl) {
                return Jsoup.parse(sampleHtml);
            }
        };

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(50);
        criteria.setMaxPriceMillion(100);

        PortalScrapeResult result = scraper.scrape(criteria);
        assertNotNull(result);
        assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getItems().size());

        IngatlanItem item = result.getItems().get(0);
        assertEquals("H987654", item.getId());
        assertEquals("https://www.oc.hu/ingatlanok/H987654", item.getUrl());
        assertEquals("Otthon Centrum", item.getPortal());
        assertNotNull(item.getPriceInfo());
        assertEquals(88.0, item.getPriceInfo().getAmountMillion());
        assertNotNull(item.getDetailsInfo());
        assertEquals(95.0, item.getDetailsInfo().getAreaSquareMeters());
    }

    @Test
    @DisplayName("OtthonCentrumScraper handles no results marker cleanly")
    void testScrapeNoResultsMarker() {
        String noResultsHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="option">Nincs találat a megadott szűrésre.</div>
            </body>
            </html>
            """;

        OtthonCentrumScraper scraper = new OtthonCentrumScraper() {
            @Override
            public Document getDocument(String targetUrl) {
                return Jsoup.parse(noResultsHtml);
            }
        };

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");

        PortalScrapeResult result = scraper.scrape(criteria);
        assertNotNull(result);
        assertEquals(PortalScrapeStatus.EMPTY, result.getStatus());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    @DisplayName("OtthonCentrumScraper handles network failure without throwing unhandled exceptions")
    void testScrapeNetworkFailure() {
        OtthonCentrumScraper scraper = new OtthonCentrumScraper() {
            @Override
            public Document getDocument(String targetUrl) {
                return null;
            }
        };

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");

        PortalScrapeResult result = scraper.scrape(criteria);
        assertNotNull(result);
        assertEquals(PortalScrapeStatus.FAILED, result.getStatus());
    }
}
