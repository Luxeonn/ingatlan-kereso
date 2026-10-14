package ingatlanok.core;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertyFilterEngineTest {

    @Test
    @DisplayName("Szűrés ár, alapterület, város és megtekintett állapot szerint")
    void testFiltering() {
        PropertyFilterEngine filterEngine = new PropertyFilterEngine();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(80);
        criteria.setMinArea(50);
        criteria.setMaxArea(120);
        criteria.setExcludeViewed(true);

        IngatlanItem match = new IngatlanItem("1", "dh", "http://1", "Jó lakás", "60 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14", false);
        IngatlanItem cheap = new IngatlanItem("2", "dh", "http://2", "Olcsó", "20 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14", false);
        IngatlanItem expensive = new IngatlanItem("3", "dh", "http://3", "Drága", "150 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14", false);
        IngatlanItem small = new IngatlanItem("4", "dh", "http://4", "Kicsi", "50 M Ft", "Debrecen", "Debrecen", "30 m²", "2026-10-14", false);
        IngatlanItem large = new IngatlanItem("5", "dh", "http://5", "Nagy", "50 M Ft", "Debrecen", "Debrecen", "200 m²", "2026-10-14", false);
        IngatlanItem viewed = new IngatlanItem("6", "dh", "http://6", "Látott", "50 M Ft", "Debrecen", "Debrecen", "70 m²", "2026-10-14", false);
        viewed.setViewed(true);
        IngatlanItem wrongCity = new IngatlanItem("7", "dh", "http://7", "Más város", "50 M Ft", "Szeged", "Szeged", "70 m²", "2026-10-14", false);

        List<IngatlanItem> items = List.of(match, cheap, expensive, small, large, viewed, wrongCity);
        PropertyFilterEngine.FilterResult result = filterEngine.filter(items, criteria, "Debrecen");

        assertEquals(1, result.matchingItems().size());
        assertEquals("http://1", result.matchingItems().get(0).getUrl());
        assertEquals(7, result.totalInputCount());
        assertEquals(2, result.filteredByPriceCount());
        assertEquals(2, result.filteredByAreaCount());
        assertEquals(1, result.filteredByViewedCount());
        assertEquals(1, result.filteredByCityCount());
    }

    @Test
    @DisplayName("Üres vagy null bemenet kezelése")
    void testEmptyOrNull() {
        PropertyFilterEngine filterEngine = new PropertyFilterEngine();
        PropertyFilterEngine.FilterResult r1 = filterEngine.filter(null, null, "Debrecen");
        assertEquals(0, r1.matchingItems().size());
        assertEquals(0, r1.totalInputCount());

        PropertyFilterEngine.FilterResult r2 = filterEngine.filter(List.of(), null, "Debrecen");
        assertEquals(0, r2.matchingItems().size());
    }
}
