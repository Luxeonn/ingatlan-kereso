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

        SearchCriteria officeCriteria = new SearchCriteria();
        officeCriteria.setCity("Debrecen");
        officeCriteria.setPropertyType("iroda");
        assertEquals("https://www.oc.hu/elado/uzleticelu/debrecen", scraper.buildSearchUrl(officeCriteria));

        SearchCriteria garageCriteria = new SearchCriteria();
        garageCriteria.setCity("Szeged");
        garageCriteria.setPropertyType("garazs");
        assertEquals("https://www.oc.hu/elado/garazs/szeged", scraper.buildSearchUrl(garageCriteria));
    }

    @Test
    @DisplayName("OtthonCentrumScraper parses cards successfully even when hidden location-option-no-result template is present")
    void testScrapeRealOcCardWithHiddenOptionNoResult() {
        String realHtmlWithHiddenNoResult = """
            <!DOCTYPE html>
            <html>
            <body>
                <!-- Autocomplete dropdown hidden template that previously broke scraping -->
                <div class="location-option-no-result d-none">
                    <div class="option">Nincs találat</div>
                </div>

                <div class="infinite-container real-estate-list">
                    <div class="py-2">297 ingatlan</div>
                    <div class="row items_container js-host">
                        <div class="col-md-4 col-xl-3 js-list-item"
                             data-controller="seo"
                             data-seo-select-item-value='{"ecommerce":{"items":[{"item_id":"H525543","item_name":"H525543","price":53500000,"location_city":"Debrecen","location_district":"Belváros"}]}}'>
                            <a target="_blank" href="/ingatlanok/H525543" data-action="seo#selectItem">
                                <div class="card-body">
                                    <img class="card-img-top lazyload"
                                         src="https://h9.ocdn.hu/watermarkedpics/thumb.jpg"
                                         data-src="https://h9.ocdn.hu/watermarkedpics/highres.jpg"
                                         loading="lazy">
                                </div>
                                <div class="card-footer">
                                    <div class="info-row">
                                        <div class="text-truncate">Debrecen</div>
                                        <div class="text-end">54m²</div>
                                    </div>
                                    <div class="info-row">
                                        <div class="text-truncate">Belváros</div>
                                        <div class="text-end">2 szoba</div>
                                    </div>
                                    <div class="info-row">
                                        <span class="price-huf">53,5M Ft</span>
                                    </div>
                                    <div class="collapse description" id="collapse-H525543">
                                        <h6>Debrecen belvárosában, 2 szobás, erkélyes lakás eladó!</h6>
                                    </div>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;

        OtthonCentrumScraper scraper = new OtthonCentrumScraper() {
            @Override
            public Document getDocument(String targetUrl) {
                return Jsoup.parse(realHtmlWithHiddenNoResult);
            }
        };

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(70);
        criteria.setMinArea(40);
        criteria.setMaxArea(80);

        PortalScrapeResult result = scraper.scrape(criteria);
        assertNotNull(result);
        assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getItems().size());

        IngatlanItem item = result.getItems().get(0);
        assertEquals("H525543", item.getId());
        assertEquals("https://www.oc.hu/ingatlanok/H525543", item.getUrl());
        assertEquals("Debrecen belvárosában, 2 szobás, erkélyes lakás eladó!", item.getTitle());
        assertEquals("https://h9.ocdn.hu/watermarkedpics/highres.jpg", item.getImageUrl());
        assertNotNull(item.getPriceInfo());
        assertEquals(53.5, item.getPriceInfo().getAmountMillion());
        assertEquals(53500000L, item.getPriceInfo().getAmountHuf());
        assertNotNull(item.getDetailsInfo());
        assertEquals(54.0, item.getDetailsInfo().getAreaSquareMeters());
        assertEquals(2.0, item.getDetailsInfo().getRoomCount());
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
