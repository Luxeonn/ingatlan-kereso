package ingatlanok.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelClassesTest {

    @Test
    @DisplayName("PortalSource mappings and metadata")
    void testPortalSource() {
        PortalSource dh = PortalSource.of("Duna House");
        assertEquals("dh", dh.getPortalId());
        assertEquals("Duna House", dh.getPortalName());
        assertTrue(dh.isDirectListing());
        assertNotNull(dh.getThemeColor());

        PortalSource oc = PortalSource.of("Otthon Centrum");
        assertEquals("oc", oc.getPortalId());
        assertEquals("Otthon Centrum", oc.getPortalName());

        PortalSource jf = PortalSource.of("Jófogás Ingatlan");
        assertEquals("jofogas", jf.getPortalId());

        PortalSource ic = PortalSource.of("ingatlan.com");
        assertEquals("ingatlancom", ic.getPortalId());
        assertFalse(ic.isDirectListing());

        PortalSource ib = PortalSource.of("Ingatlanbazár");
        assertEquals("ingatlanbazar", ib.getPortalId());

        PortalSource unknown = PortalSource.of(null);
        assertEquals("unknown", unknown.getPortalId());
    }

    @Test
    @DisplayName("PriceInfo construction and formatting")
    void testPriceInfo() {
        PriceInfo priceInfo = new PriceInfo("45.5 M Ft", 45_500_000L, 45.5);
        assertEquals(45_500_000L, priceInfo.getAmountHuf());
        assertEquals(45.5, priceInfo.getAmountMillion());
        assertEquals("45.5 M Ft", priceInfo.getRawText());
        assertEquals("HUF", priceInfo.getCurrency());
        assertTrue(priceInfo.toString().contains("45.5"));

        PriceInfo emptyInfo = new PriceInfo();
        assertNull(emptyInfo.getRawText());
    }

    @Test
    @DisplayName("PropertyDetails sqm and room count")
    void testPropertyDetails() {
        PropertyDetails details = new PropertyDetails("60 m²", 60.0, 2.5, 750_000L);
        assertEquals("60 m²", details.getRawText());
        assertEquals(60.0, details.getAreaSquareMeters());
        assertEquals(2.5, details.getRoomCount());
        assertEquals(750_000L, details.getPricePerSquareMeter());
        assertTrue(details.toString().contains("60"));
    }

    @Test
    @DisplayName("IngatlanItem equality, setters, getters, and flags")
    void testIngatlanItem() {
        IngatlanItem item1 = new IngatlanItem();
        item1.setUrl("https://dh.hu/ingatlan/123");
        item1.setTitle("Szép lakás");
        item1.setPrice("50 M Ft");
        item1.setCity("Debrecen");
        item1.setPortal("Duna House");
        item1.setNew(true);
        item1.setDiscoveredAt("2026-10-13");
        item1.setDetails("55 m²");

        assertEquals("https://dh.hu/ingatlan/123", item1.getUrl());
        assertEquals("Szép lakás", item1.getTitle());
        assertEquals("50 M Ft", item1.getPrice());
        assertEquals("Debrecen", item1.getCity());
        assertEquals("Duna House", item1.getPortal());
        assertTrue(item1.isNew());
        assertEquals("2026-10-13", item1.getDiscoveredAt());
        assertEquals("55 m²", item1.getDetails());

        IngatlanItem item2 = new IngatlanItem();
        item2.setUrl("https://dh.hu/ingatlan/123");

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotNull(item1.toString());

        PriceInfo priceInfo = new PriceInfo("50 M Ft", 50_000_000L, 50.0);
        PropertyDetails details = new PropertyDetails("55 m²", 55.0, 2.0, 909_090L);
        item1.setPriceInfo(priceInfo);
        item1.setDetailsInfo(details);

        assertEquals(priceInfo, item1.getPriceInfo());
        assertEquals(details, item1.getDetailsInfo());
    }

    @Test
    @DisplayName("SearchResult aggregation and portal statistics")
    void testSearchResult() {
        IngatlanItem item1 = new IngatlanItem();
        item1.setUrl("https://dh.hu/1");
        item1.setPortal("Duna House");
        item1.setNew(true);

        IngatlanItem item2 = new IngatlanItem();
        item2.setUrl("https://oc.hu/2");
        item2.setPortal("Otthon Centrum");
        item2.setNew(false);

        SearchResult result = new SearchResult();
        result.setItems(List.of(item1, item2));
        result.setPortalCounts(Map.of("dh", 1, "oc", 1));
        result.setTotalFound(2);
        result.setNewCount(1);

        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getTotalFound());
        assertEquals(1, result.getNewCount());
        assertEquals(1, result.getPortalCounts().get("dh"));
        assertEquals(1, result.getPortalCounts().get("oc"));
    }

    @Test
    @DisplayName("SearchCriteria area and price defaults and clone")
    void testSearchCriteriaModel() {
        SearchCriteria criteria = new SearchCriteria();
        assertEquals(70, criteria.getMinPriceMillion());
        assertEquals(120, criteria.getMaxPriceMillion());
        assertEquals(90, criteria.getMinArea());
        assertEquals(200, criteria.getMaxArea());

        criteria.setMinArea(60);
        criteria.setMaxArea(180);
        assertEquals(60, criteria.getMinArea());
        assertEquals(180, criteria.getMaxArea());

        SearchCriteria clone = criteria.cloneForCity("Miskolc");
        assertEquals("Miskolc", clone.getCity());
        assertEquals(60, clone.getMinArea());
        assertEquals(180, clone.getMaxArea());
        assertEquals(70, clone.getMinPriceMillion());
        assertEquals(120, clone.getMaxPriceMillion());
    }
}
