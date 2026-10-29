package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

class PropertyFilterEngineTest {

  @Test
  @DisplayName("Szűrés ár, alapterület, város és megtekintett állapot szerint")
  void testFiltering() {
    PropertyFilterEngine filterEngine = new PropertyFilterEngine();

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(80);
    criteria.setMinArea(50);
    criteria.setMaxArea(120);
    criteria.setExcludeViewed(true);

    IngatlanItem match =
        new IngatlanItem(
            "1",
            "dh",
            "http://1",
            "Jó lakás",
            "60 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    IngatlanItem cheap =
        new IngatlanItem(
            "2",
            "dh",
            "http://2",
            "Olcsó",
            "20 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    IngatlanItem expensive =
        new IngatlanItem(
            "3",
            "dh",
            "http://3",
            "Drága",
            "150 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    IngatlanItem small =
        new IngatlanItem(
            "4",
            "dh",
            "http://4",
            "Kicsi",
            "50 M Ft",
            "Debrecen",
            "Debrecen",
            "30 m²",
            "2026-10-14",
            false);
    IngatlanItem large =
        new IngatlanItem(
            "5",
            "dh",
            "http://5",
            "Nagy",
            "50 M Ft",
            "Debrecen",
            "Debrecen",
            "200 m²",
            "2026-10-14",
            false);
    IngatlanItem viewed =
        new IngatlanItem(
            "6",
            "dh",
            "http://6",
            "Látott",
            "50 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    viewed.setViewed(true);
    IngatlanItem wrongCity =
        new IngatlanItem(
            "7",
            "dh",
            "http://7",
            "Más város",
            "50 M Ft",
            "Szeged",
            "Szeged",
            "70 m²",
            "2026-10-14",
            false);

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

  @Test
  @DisplayName("Szűrés kizárólag leárazott (onlyDiscounted) ingatlanokra")
  void testOnlyDiscountedFilter() {
    PropertyFilterEngine filterEngine = new PropertyFilterEngine();

    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(80);
    criteria.setMinArea(50);
    criteria.setMaxArea(120);
    criteria.setOnlyDiscounted(true);

    IngatlanItem normal =
        new IngatlanItem(
            "1",
            "dh",
            "http://normal",
            "Normál lakás",
            "60 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    normal.setPriceDelta(0L);

    IngatlanItem discounted =
        new IngatlanItem(
            "2",
            "dh",
            "http://discounted",
            "Leárazott lakás",
            "55 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    discounted.setPriceDelta(-5_000_000L);
    discounted.setPriceChangePercent(-8.33);

    IngatlanItem increased =
        new IngatlanItem(
            "3",
            "dh",
            "http://increased",
            "Drágult lakás",
            "65 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-14",
            false);
    increased.setPriceDelta(5_000_000L);

    PropertyFilterEngine.FilterResult result =
        filterEngine.filter(List.of(normal, discounted, increased), criteria, "Debrecen");
    assertEquals(1, result.matchingItems().size());
    assertEquals("http://discounted", result.matchingItems().get(0).getUrl());
    assertEquals(2, result.filteredByDiscountCount());
  }

  @Test
  @DisplayName("Szűrés tetszőleges (nem 5/10-zel osztható) egyedi ár- és négyzetméter értékekre")
  void testArbitraryPriceAndAreaBounds() {
    PropertyFilterEngine filterEngine = new PropertyFilterEngine();

    SearchCriteria criteria = new SearchCriteria();
    criteria.setMinPriceMillion(73);
    criteria.setMaxPriceMillion(107);
    criteria.setMinArea(63);
    criteria.setMaxArea(77);

    IngatlanItem match =
        new IngatlanItem(
            "1",
            "dh",
            "http://match",
            "Pont jó",
            "73 M Ft",
            "Debrecen",
            "Debrecen",
            "63 m²",
            "2026-10-18",
            false);
    IngatlanItem matchUpper =
        new IngatlanItem(
            "2",
            "dh",
            "http://matchUpper",
            "Pont jó felső",
            "107 M Ft",
            "Debrecen",
            "Debrecen",
            "77 m²",
            "2026-10-18",
            false);
    IngatlanItem belowPrice =
        new IngatlanItem(
            "3",
            "dh",
            "http://belowPrice",
            "Alatta ár",
            "72.9 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-18",
            false);
    IngatlanItem abovePrice =
        new IngatlanItem(
            "4",
            "dh",
            "http://abovePrice",
            "Felette ár",
            "107.1 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-18",
            false);
    IngatlanItem belowArea =
        new IngatlanItem(
            "5",
            "dh",
            "http://belowArea",
            "Alatta terület",
            "90 M Ft",
            "Debrecen",
            "Debrecen",
            "62 m²",
            "2026-10-18",
            false);
    IngatlanItem aboveArea =
        new IngatlanItem(
            "6",
            "dh",
            "http://aboveArea",
            "Felette terület",
            "90 M Ft",
            "Debrecen",
            "Debrecen",
            "78 m²",
            "2026-10-18",
            false);

    List<IngatlanItem> items =
        List.of(match, matchUpper, belowPrice, abovePrice, belowArea, aboveArea);
    PropertyFilterEngine.FilterResult result = filterEngine.filter(items, criteria, "Debrecen");

    assertEquals(2, result.matchingItems().size());
    assertTrue(result.matchingItems().stream().anyMatch(i -> "http://match".equals(i.getUrl())));
    assertTrue(
        result.matchingItems().stream().anyMatch(i -> "http://matchUpper".equals(i.getUrl())));
    assertEquals(2, result.filteredByPriceCount());
    assertEquals(2, result.filteredByAreaCount());
  }

  @Test
  @DisplayName("Szűrés kizárólag új építésű (onlyNewConstruction) ingatlanokra")
  void testOnlyNewConstructionFilter() {
    PropertyFilterEngine filterEngine = new PropertyFilterEngine();

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(40);
    criteria.setMaxArea(150);
    criteria.setOnlyNewConstruction(true);

    IngatlanItem usedHome =
        new IngatlanItem(
            "1",
            "dh",
            "http://used",
            "Használt lakás",
            "60 M Ft",
            "Debrecen",
            "Debrecen",
            "70 m²",
            "2026-10-18",
            false);
    usedHome.setNewConstruction(false);

    IngatlanItem newBuild =
        new IngatlanItem(
            "2",
            "dh",
            "http://newbuild",
            "Új építésű penthouse",
            "95 M Ft",
            "Debrecen",
            "Debrecen",
            "85 m²",
            "2026-10-18",
            true);
    newBuild.setNewConstruction(true);

    PropertyFilterEngine.FilterResult result =
        filterEngine.filter(List.of(usedHome, newBuild), criteria, "Debrecen");
    assertEquals(1, result.matchingItems().size());
    assertEquals("http://newbuild", result.matchingItems().get(0).getUrl());
    assertEquals(1, result.filteredByNewConstructionCount());
    org.junit.jupiter.api.Assertions.assertFalse(criteria.matches(usedHome));
    assertTrue(criteria.matches(newBuild));
    assertTrue(criteria.matchesRelevance(usedHome));
    assertTrue(criteria.matchesRelevance(newBuild));
    org.junit.jupiter.api.Assertions.assertFalse(criteria.matchesView(usedHome));
    assertTrue(criteria.matchesView(newBuild));

    PropertyFilterEngine.FilterResult relevanceOnly =
        filterEngine.filterRelevance(List.of(usedHome, newBuild), criteria, "Debrecen");
    assertEquals(2, relevanceOnly.matchingItems().size());
    assertEquals(0, relevanceOnly.filteredByNewConstructionCount());

    PropertyFilterEngine.FilterResult viewOnly =
        filterEngine.filterView(relevanceOnly.matchingItems(), criteria);
    assertEquals(1, viewOnly.matchingItems().size());
    assertEquals(1, viewOnly.filteredByNewConstructionCount());
  }
}
