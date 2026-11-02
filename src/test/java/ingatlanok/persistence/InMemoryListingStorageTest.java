package ingatlanok.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

class InMemoryListingStorageTest {

  private InMemoryListingStorage storage;

  @BeforeEach
  void setUp() {
    storage = new InMemoryListingStorage();
  }

  @Test
  @DisplayName("saveCityItems and basic querying")
  void testSaveCityItemsAndQuery() {
    IngatlanItem item =
        new IngatlanItem(
            "p1",
            "dh",
            "https://dh.hu/1",
            "Szép lakás",
            "50 M Ft",
            "Debrecen",
            "60 m²",
            "2026-10-20",
            true);

    storage.saveCityItems("Debrecen", List.of(item));

    assertEquals(1, storage.getTotalCount());
    assertTrue(storage.hasCityDatabase("Debrecen"));
    assertFalse(storage.hasCityDatabase("Szeged"));
    assertTrue(storage.containsUrl("https://dh.hu/1"));
    assertFalse(storage.containsUrl("https://dh.hu/non-existing"));
    assertEquals(1, storage.getItemsByCity("Debrecen").size());
    assertEquals(0, storage.getItemsByCity("Szeged").size());
    assertEquals(1, storage.getAll().size());
    assertEquals(1, storage.getAllUrls().size());
    assertNotNull(storage.getLastRunTime());

    List<Map<String, Object>> tracked = storage.getTrackedCitiesInfo();
    assertEquals(1, tracked.size());
    assertEquals("debrecen", tracked.get(0).get("key"));

    assertTrue(storage.markAsViewed("https://dh.hu/1"));
    assertTrue(storage.isViewed("https://dh.hu/1"));
    assertFalse(storage.markAsViewed("https://dh.hu/non-existing"));
    assertFalse(storage.isViewed("https://dh.hu/non-existing"));

    storage.clearCityHistory("Debrecen");
    assertEquals(0, storage.getTotalCount());
    assertFalse(storage.hasCityDatabase("Debrecen"));
  }

  @Test
  @DisplayName("saveAll and clearHistory")
  void testSaveAllAndClearHistory() {
    IngatlanItem item1 =
        new IngatlanItem(
            "p1", "dh", "https://dh.hu/1", "Lakás", "50 M Ft", "Debrecen", "60 m²", "2026-10-20", false);
    item1.setViewed(true);
    IngatlanItem item2 =
        new IngatlanItem(
            "p2", "oc", "https://oc.hu/2", "Ház", "70 M Ft", "Szeged", "90 m²", "2026-10-20", false);

    storage.saveAll(List.of(item1, item2));
    assertEquals(2, storage.getTotalCount());
    assertTrue(storage.isViewed("https://dh.hu/1"));
    assertFalse(storage.isViewed("https://oc.hu/2"));

    storage.clearHistory();
    assertEquals(0, storage.getTotalCount());
    assertEquals(0, storage.getAll().size());
  }

  @Test
  @DisplayName("Criteria and unsupported methods handle defaults safely")
  void testCriteriaAndUnsupported() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    storage.saveLastCriteria(criteria);
    assertEquals("Debrecen", storage.getLastCriteria().getCity());

    assertEquals(Collections.emptyList(), storage.getPriceHistory("any"));
    assertEquals(Collections.emptyList(), storage.getStatusLog("any"));
    assertEquals(Collections.emptyList(), storage.getDiscountedProperties("any"));
    assertEquals(Collections.emptyList(), storage.findByCriteria(null));
    assertEquals(Map.of(), storage.countByPortal());

    assertFalse(storage.isNewUrl("Debrecen", null));
    assertFalse(storage.isNewUrl("Debrecen", "https://dh.hu/1"));
    assertFalse(storage.markAsViewed(null));
    assertFalse(storage.isViewed(null));
    assertEquals(Collections.emptyList(), storage.getItemsByCity(null));
    assertEquals(Map.of(), storage.findByUrls(null));
    assertEquals(Map.of(), storage.findByUrls(List.of()));
    assertEquals(Map.of(), storage.findByUrls(List.of(" ", "")));
  }
}
