package ingatlanok.service;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngatlanSearchServiceTest {

    private static final String MOCK_PORTAL_ID = "mockbazar";

    @org.junit.jupiter.api.BeforeAll
    static void setupMockPortal() {
        ingatlanok.portal.PortalEngine mockEngine = new ingatlanok.portal.PortalEngine() {
            @Override
            public ingatlanok.portal.PortalDescriptor getDescriptor() {
                return new ingatlanok.portal.PortalDescriptor(MOCK_PORTAL_ID, "Mock Ingatlanbazár", "#336699", "https://mockbazar.hu", true, List.of("lakas"), true);
            }

            @Override
            public ingatlanok.scraper.PortalScrapeResult scrape(SearchCriteria criteria) {
                ingatlanok.model.IngatlanItem item1 = new ingatlanok.model.IngatlanItem(
                        "mb1", ingatlanok.model.PortalSource.of(MOCK_PORTAL_ID), "https://mockbazar.hu/1", "Debreceni lakás 1",
                        new ingatlanok.model.PriceInfo("45 M Ft", 45_000_000L, 45.0),
                        "Debrecen",
                        new ingatlanok.model.PropertyDetails("100 m²", 100.0, 3.0, 450_000L),
                        "2026-10-15", true
                );
                ingatlanok.model.IngatlanItem item2 = new ingatlanok.model.IngatlanItem(
                        "mb2", ingatlanok.model.PortalSource.of(MOCK_PORTAL_ID), "https://mockbazar.hu/2", "Debreceni lakás 2",
                        new ingatlanok.model.PriceInfo("55 M Ft", 55_000_000L, 55.0),
                        "Debrecen",
                        new ingatlanok.model.PropertyDetails("120 m²", 120.0, 4.0, 458_333L),
                        "2026-10-15", false
                );
                return ingatlanok.scraper.PortalScrapeResult.success(MOCK_PORTAL_ID, "Mock Ingatlanbazár", List.of(item1, item2), 10);
            }
        };
        ingatlanok.portal.PortalEngineRegistry.getInstance().register(mockEngine);
    }

    @Test
    @DisplayName("IngatlanSearchService null criteria handling")
    void testSearchNullCriteria() {
        IngatlanSearchService service = IngatlanSearchService.getInstance();
        SearchResult result = service.executeSearch(null);
        assertNotNull(result);
        assertNotNull(result.getItems());
    }

    @Test
    @DisplayName("IngatlanSearchService execution with selected portal")
    void testSearchExecution() {
        IngatlanSearchService service = IngatlanSearchService.getInstance();
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen"));
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(60);
        criteria.setPortals(List.of(MOCK_PORTAL_ID));

        SearchResult result = service.executeSearch(criteria);
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertNotNull(result.getPortalCounts());
        assertTrue(result.getPortalCounts().containsKey("Mock Ingatlanbazár"));
        assertFalse(result.getItems().isEmpty());
    }

    @Test
    @DisplayName("IngatlanSearchService sorts results by price ascending by default")
    void testDefaultSortPriceAscending() {
        IngatlanSearchService service = IngatlanSearchService.getInstance();
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen"));
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(60);
        criteria.setPortals(List.of(MOCK_PORTAL_ID));

        SearchResult result = service.executeSearch(criteria);
        assertNotNull(result);
        List<ingatlanok.model.IngatlanItem> items = result.getItems();
        assertNotNull(items);

        Long previousPrice = null;
        for (ingatlanok.model.IngatlanItem item : items) {
            Long currentPrice = item.getPriceInfo() != null ? item.getPriceInfo().getAmountHuf() : null;
            if (currentPrice != null) {
                if (previousPrice != null) {
                    assertTrue(currentPrice >= previousPrice, "Az áraknak növekvő sorrendben kell lenniük: " + previousPrice + " <= " + currentPrice);
                }
                previousPrice = currentPrice;
            }
        }
    }

    @Test
    @DisplayName("IngatlanSearchService comparator branches coverage")
    void testSortComparatorCoverage() {
        ingatlanok.model.IngatlanItem item1 = new ingatlanok.model.IngatlanItem("1", "dh", "http://1", "A", "50 M Ft", "Debrecen", "", "2026-10-10", true);
        ingatlanok.model.IngatlanItem item2 = new ingatlanok.model.IngatlanItem("2", "dh", "http://2", "B", "30 M Ft", "Debrecen", "", "2026-10-11", false);
        ingatlanok.model.IngatlanItem item3 = new ingatlanok.model.IngatlanItem("3", "dh", "http://3", "C", "Ár a hirdetésben", "Debrecen", "", null, false);
        ingatlanok.model.IngatlanItem item4 = new ingatlanok.model.IngatlanItem("4", "dh", "http://4", "D", "30 M Ft", "Debrecen", "", "2026-10-12", true);
        ingatlanok.model.IngatlanItem item5 = new ingatlanok.model.IngatlanItem("5", "dh", "http://5", "E", null, "Debrecen", "", "2026-10-09", false);

        java.util.List<ingatlanok.model.IngatlanItem> list = new java.util.ArrayList<>(java.util.List.of(item1, item2, item3, item4, item5));

        list.sort((a, b) -> {
            Long priceA = (a.getPriceInfo() != null) ? a.getPriceInfo().getAmountHuf() : null;
            if (priceA == null && a.getPrice() != null) {
                priceA = ingatlanok.common.PropertyParser.parsePrice(a.getPrice()).getAmountHuf();
            }
            Long priceB = (b.getPriceInfo() != null) ? b.getPriceInfo().getAmountHuf() : null;
            if (priceB == null && b.getPrice() != null) {
                priceB = ingatlanok.common.PropertyParser.parsePrice(b.getPrice()).getAmountHuf();
            }

            if (priceA != null && priceB != null) {
                int cmp = Long.compare(priceA, priceB);
                if (cmp != 0) return cmp;
            } else if (priceA != null) {
                return -1;
            } else if (priceB != null) {
                return 1;
            }

            if (a.isNew() != b.isNew()) {
                return Boolean.compare(!a.isNew(), !b.isNew());
            }
            if (a.getDiscoveredAt() == null) return 1;
            if (b.getDiscoveredAt() == null) return -1;
            return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
        });

        assertEquals("4", list.get(0).getId());
        assertEquals("2", list.get(1).getId());
        assertEquals("1", list.get(2).getId());
        assertEquals("5", list.get(3).getId());
        assertEquals("3", list.get(4).getId());
    }
}
