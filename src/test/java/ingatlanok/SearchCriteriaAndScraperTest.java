package ingatlanok;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.service.impl.DunaHause;
import ingatlanok.service.impl.IngatlanCom;
import ingatlanok.service.impl.JofogasIngatlan;
import ingatlanok.service.impl.OtthonCentrum;
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
    void testIngatlanComSearchAndFallback() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(80);

        IngatlanCom ic = new IngatlanCom();
        List<IngatlanItem> items = ic.search(criteria);

        assertNotNull(items);
        assertFalse(items.isEmpty(), "Ingatlan.com should return at least direct link item on Cloudflare fallback");

        IngatlanItem first = items.get(0);
        assertEquals("Ingatlan.com", first.getPortal());
        assertTrue(first.getUrl().contains("ingatlan.com/lista/elado+haz+debrecen+40-80-mFt"),
                "URL should contain mapped type, city and price: " + first.getUrl());
    }

    @Test
    void testScraperExecutionRobustness() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(25);
        criteria.setMaxPriceMillion(50);

        assertDoesNotThrow(() -> {
            List<IngatlanItem> dh = new DunaHause().search(criteria);
            assertNotNull(dh);
        });

        assertDoesNotThrow(() -> {
            List<IngatlanItem> oc = new OtthonCentrum().search(criteria);
            assertNotNull(oc);
        });

        assertDoesNotThrow(() -> {
            List<IngatlanItem> jf = new JofogasIngatlan().search(criteria);
            assertNotNull(jf);
        });
    }
}
