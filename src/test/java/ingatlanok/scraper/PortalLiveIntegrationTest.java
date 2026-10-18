package ingatlanok.scraper;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("live")
class PortalLiveIntegrationTest {

    private SearchCriteria createCriteria(String city, String type, int minPrice, int maxPrice) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity(city);
        criteria.setPropertyType(type);
        criteria.setMinPriceMillion(minPrice);
        criteria.setMaxPriceMillion(maxPrice);
        return criteria;
    }

    @Test
    @DisplayName("Duna House live single query test")
    void testDunaHouseLiveSearch() {
        SearchCriteria criteria = createCriteria("Budapest", "lakas", 40, 80);
        DunaHouseScraper scraper = new DunaHouseScraper();

        assertEquals("dh", scraper.getPortalId());
        assertEquals("Duna House", scraper.getName());

        assertDoesNotThrow(() -> {
            List<IngatlanItem> items = scraper.search(criteria);
            assertNotNull(items);
            if (!items.isEmpty()) {
                IngatlanItem sample = items.getFirst();
                assertEquals("Duna House", sample.getPortal());
                assertNotNull(sample.getUrl());
                assertTrue(sample.getUrl().startsWith("http"));
                assertTrue(sample.getUrl().contains("dh.hu"));
                assertNotNull(sample.getTitle());
            }
        });
    }

    @Test
    @DisplayName("Otthon Centrum live single query test")
    void testOtthonCentrumLiveSearch() {
        SearchCriteria criteria = createCriteria("Budapest", "lakas", 40, 80);
        OtthonCentrumScraper scraper = new OtthonCentrumScraper();

        assertEquals("oc", scraper.getPortalId());
        assertEquals("Otthon Centrum", scraper.getName());

        assertDoesNotThrow(() -> {
            List<IngatlanItem> items = scraper.search(criteria);
            assertNotNull(items);
            if (!items.isEmpty()) {
                IngatlanItem sample = items.getFirst();
                assertEquals("Otthon Centrum", sample.getPortal());
                assertNotNull(sample.getUrl());
                assertTrue(sample.getUrl().startsWith("http"));
                assertTrue(sample.getUrl().contains("oc.hu"));
            }
        });
    }

    @Test
    @DisplayName("Jofogas live single query test")
    void testJofogasLiveSearch() {
        SearchCriteria criteria = createCriteria("Debrecen", "lakas", 30, 60);
        JofogasScraper scraper = new JofogasScraper();

        assertEquals("jofogas", scraper.getPortalId());
        assertEquals("Jófogás Ingatlan", scraper.getName());

        assertDoesNotThrow(() -> {
            List<IngatlanItem> items = scraper.search(criteria);
            assertNotNull(items);
            if (!items.isEmpty()) {
                IngatlanItem sample = items.getFirst();
                assertEquals("Jófogás Ingatlan", sample.getPortal());
                assertNotNull(sample.getUrl());
                assertTrue(sample.getUrl().startsWith("http"));
                assertTrue(sample.getUrl().contains("jofogas.hu"));
            }
        });
    }

    @Test
    @DisplayName("Ingatlanbazar live single query test")
    void testIngatlanBazarLiveSearch() {
        SearchCriteria criteria = createCriteria("Debrecen", "lakas", 30, 70);
        IngatlanBazarScraper scraper = new IngatlanBazarScraper();

        assertEquals("ingatlanbazar", scraper.getPortalId());
        assertEquals("Ingatlanbazár", scraper.getName());

        assertDoesNotThrow(() -> {
            List<IngatlanItem> items = scraper.search(criteria);
            assertNotNull(items);
            assertFalse(items.isEmpty());
            IngatlanItem sample = items.getFirst();
            assertEquals("Ingatlanbazár", sample.getPortal());
            assertNotNull(sample.getUrl());
            assertTrue(sample.getUrl().startsWith("http"));
            assertTrue(sample.getUrl().contains("ingatlanbazar.hu"));
            assertNotNull(sample.getPrice());
            assertFalse(sample.getPrice().isBlank());
        });
    }

    @Test
    @DisplayName("Ingatlan.com live single query test")
    void testIngatlanComLiveSearch() {
        SearchCriteria criteria = createCriteria("Debrecen", "haz", 40, 80);
        IngatlanComScraper scraper = new IngatlanComScraper();

        assertEquals("ingatlancom", scraper.getPortalId());
        assertEquals("Ingatlan.com", scraper.getName());

        assertDoesNotThrow(() -> {
            List<IngatlanItem> items = scraper.search(criteria);
            assertNotNull(items);
            for (IngatlanItem item : items) {
                assertEquals("Ingatlan.com", item.getPortal());
                assertNotNull(item.getUrl());
                assertTrue(item.getUrl().startsWith("http"));
                assertTrue(item.getUrl().contains("ingatlan.com"));
                assertTrue(item.getUrl().contains("/ingatlan/"));
            }
        });
    }

    @Test
    @DisplayName("PortalScrapeResult structure and detailed scrape test")
    void testDetailedScrapeResults() {
        SearchCriteria criteria = createCriteria("Debrecen", "lakas", 30, 70);
        IngatlanBazarScraper scraper = new IngatlanBazarScraper();

        PortalScrapeResult result = scraper.scrape(criteria);
        assertNotNull(result);
        assertEquals("ingatlanbazar", result.getPortalId());
        assertEquals("Ingatlanbazár", result.getPortalName());
        assertNotNull(result.getItems());
        assertNotNull(result.getStatus());
        assertNotNull(result.getMessage());
    }
}
