package ingatlanok;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.IngatlanBazarLocationResolver;
import ingatlanok.scraper.IngatlanBazarScraper;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.PortalScrapeStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IngatlanBazarScraperTest {

    private static final Logger log = LoggerFactory.getLogger(IngatlanBazarScraperTest.class);

    private final IngatlanBazarScraper scraper = new IngatlanBazarScraper();

    @Test
    void testLocationResolverKnownCities() {
        Optional<Integer> debrecenId = IngatlanBazarLocationResolver.resolveLocationId("Debrecen");
        assertTrue(debrecenId.isPresent(), "Debrecen ID must be resolved");
        assertEquals(1435, debrecenId.get());

        Optional<Integer> bpId = IngatlanBazarLocationResolver.resolveLocationId("Budapest");
        assertTrue(bpId.isPresent(), "Budapest ID must be resolved");
        assertEquals(1050, bpId.get());

        Optional<Integer> szegedId = IngatlanBazarLocationResolver.resolveLocationId("Szeged");
        assertTrue(szegedId.isPresent(), "Szeged ID must be resolved");
        assertEquals(1099, szegedId.get());

        Optional<Integer> szoboszloId = IngatlanBazarLocationResolver.resolveLocationId("Hajdúszoboszló");
        assertTrue(szoboszloId.isPresent(), "Hajdúszoboszló ID must be resolved");
        assertEquals(1445, szoboszloId.get());
    }

    @Test
    void testBuildSearchUrl() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(80);
        criteria.setMaxPriceMillion(120);

        String url = scraper.buildSearchUrl(criteria);
        assertNotNull(url);
        assertTrue(url.contains("ingatlan-Debrecen-Elado-Haz"), "Web URL must contain city and property type slug");
        assertTrue(url.contains("property_location=1435"), "Web URL must contain Debrecen location ID");
        assertTrue(url.contains("price_min=80"), "Web URL must contain price_min");
        assertTrue(url.contains("price_max=120"), "Web URL must contain price_max");
        assertTrue(url.contains("property__2=2"), "Web URL must contain house type parameter");
    }

    @Test
    void testBuildApiSearchUrl() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(50);
        criteria.setMaxPriceMillion(90);

        String apiUrl = scraper.buildApiSearchUrl(criteria, 1);
        assertNotNull(apiUrl);
        assertTrue(apiUrl.startsWith("https://www.ingatlanbazar.hu/api/property-search"));
        assertTrue(apiUrl.contains("property__1=1"), "API URL must contain property__1=1");
        assertTrue(apiUrl.contains("property_location=1435"), "API URL must contain location id 1435");
        assertTrue(apiUrl.contains("price_min=50"));
        assertTrue(apiUrl.contains("price_max=90"));
        assertTrue(apiUrl.contains("property__2=3"), "Apartment type parameter must be 3");
        assertTrue(apiUrl.contains("limit=50"));
        assertTrue(apiUrl.contains("page=1"));
    }

    @Test
    void testBuildSearchAndApiUrlTelek() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("telek");
        criteria.setMinPriceMillion(10);
        criteria.setMaxPriceMillion(30);

        String url = scraper.buildSearchUrl(criteria);
        assertTrue(url.contains("ingatlan-Debrecen-Elado-Telek"));
        assertTrue(url.contains("property__2=5"));

        String apiUrl = scraper.buildApiSearchUrl(criteria, 1);
        assertTrue(apiUrl.contains("property__2=5"));
    }

    @Test
    void testParseItemsFromJson() {
        String mockJson = """
                {
                  "hits": [
                    {
                      "propertyId": "test-uuid-1234",
                      "title": "Kiváló állapotú ház Debrecen központjában",
                      "priceHUF": 95000000,
                      "sqmPriceHuf": 950000,
                      "area": 100,
                      "rooms": 4,
                      "seoUrl": "/debrecen-elado-haz/test-uuid-1234",
                      "settlement": "Debrecen",
                      "quarter": "Belváros",
                      "createdAt": "2026-10-13 10:00:00"
                    }
                  ]
                }
                """;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");

        List<IngatlanItem> items = scraper.parseItemsFromJson(mockJson, criteria, new HashSet<>(), "2026-10-13 12:00:00");
        assertNotNull(items);
        assertEquals(1, items.size());

        IngatlanItem item = items.get(0);
        assertEquals("test-uuid-1234", item.getId());
        assertEquals("Ingatlanbazár", item.getPortal());
        assertTrue(item.getUrl().contains("/debrecen-elado-haz/test-uuid-1234"));
        assertEquals("Kiváló állapotú ház Debrecen központjában", item.getTitle());
        assertEquals("Debrecen", item.getCity());
        assertEquals("Debrecen, Belváros", item.getLocation());

        assertNotNull(item.getPriceInfo());
        assertEquals(95000000L, item.getPriceInfo().getAmountHuf());
        assertEquals(95.0, item.getPriceInfo().getAmountMillion());

        assertNotNull(item.getDetailsInfo());
        assertEquals(100.0, item.getDetailsInfo().getAreaSquareMeters());
        assertEquals(4.0, item.getDetailsInfo().getRoomCount());
        assertEquals(950000L, item.getDetailsInfo().getPricePerSquareMeter());
    }

    @Test
    @org.junit.jupiter.api.Tag("live")
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_LIVE_TESTS", matches = "true")
    void testLiveScrapeDebrecen() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(80);

        PortalScrapeResult result = scraper.scrape(criteria);
        assertNotNull(result);
        assertEquals("ingatlanbazar", result.getPortalId());
        assertEquals("Ingatlanbazár", result.getPortalName());

        if (result.getStatus() == PortalScrapeStatus.SUCCESS) {
            assertFalse(result.getItems().isEmpty(), "Debrecen 40-80 M Ft lakás keresésre kell lennie találatnak");
            IngatlanItem first = result.getItems().get(0);
            assertNotNull(first.getUrl());
            assertTrue(first.getUrl().startsWith("https://www.ingatlanbazar.hu"));
            assertNotNull(first.getTitle());
            assertNotNull(first.getPriceInfo());
            log.info("Élő Ingatlanbazár találat: {} | {} | {}", first.getTitle(), first.getPrice(), first.getUrl());
        }
    }
}
