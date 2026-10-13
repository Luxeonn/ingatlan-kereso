package ingatlanok;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.DunaHouseScraper;
import ingatlanok.scraper.IngatlanComScraper;
import ingatlanok.scraper.JofogasScraper;
import ingatlanok.scraper.OtthonCentrumScraper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchCriteriaAndScraperTest {

    @Test
    void testSearchCriteriaDefaultsAndPortals() {
        SearchCriteria criteria = new SearchCriteria();

        assertEquals("Debrecen", criteria.getCity());
        assertEquals("lakas", criteria.getPropertyType());
        assertTrue(criteria.isPortalEnabled("dh"));
        assertTrue(criteria.isPortalEnabled("oc"));
        assertTrue(criteria.isPortalEnabled("jofogas"));
        assertTrue(criteria.isPortalEnabled("ingatlancom"), "Ingatlan.com should be enabled by default");

        criteria.setMinPriceMillion(30);
        criteria.setMaxPriceMillion(60);
        criteria.setPropertyType("garazs");

        assertEquals(30, criteria.getMinPriceMillion());
        assertEquals(60, criteria.getMaxPriceMillion());
        assertEquals("garazs", criteria.getPropertyType());
    }

    @Test
    void testIngatlanComSearchRobustness() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(80);

        IngatlanComScraper ic = new IngatlanComScraper();
        assertDoesNotThrow(() -> {
            List<IngatlanItem> items = ic.search(criteria);
            assertNotNull(items);
            for (IngatlanItem item : items) {
                assertEquals("Ingatlan.com", item.getPortal());
                assertTrue(item.getUrl().contains("/ingatlan/"), "Item URL must be a real property listing URL: " + item.getUrl());
                assertFalse(item.getUrl().contains("/lista/"), "Item URL must NOT be a search result page URL: " + item.getUrl());
            }
        });
    }

    @Test
    void testScraperExecutionRobustness() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(25);
        criteria.setMaxPriceMillion(50);

        assertDoesNotThrow(() -> {
            List<IngatlanItem> dh = new DunaHouseScraper().search(criteria);
            assertNotNull(dh);
        });

        assertDoesNotThrow(() -> {
            List<IngatlanItem> oc = new OtthonCentrumScraper().search(criteria);
            assertNotNull(oc);
        });

        assertDoesNotThrow(() -> {
            List<IngatlanItem> jf = new JofogasScraper().search(criteria);
            assertNotNull(jf);
        });
    }
}
