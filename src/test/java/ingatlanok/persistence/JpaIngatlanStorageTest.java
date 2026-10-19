package ingatlanok.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.StatusLogEntry;

@SpringBootTest
@DisplayName("JpaIngatlanStorage Integration Tests")
class JpaIngatlanStorageTest {

  @Autowired private JpaIngatlanStorage storage;

  @BeforeEach
  void setUp() {
    storage.clearHistory();
  }

  private IngatlanItem createItem(
      String id,
      String url,
      String portal,
      String city,
      String title,
      String priceRaw,
      long priceAmount,
      double priceMillions,
      String details,
      double areaSqm,
      double rooms,
      String discoveredAt) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setUrl(url);
    item.setPortal(portal);
    item.setCity(city);
    item.setTitle(title);
    item.setPrice(priceRaw);
    item.setPriceInfo(new PriceInfo(priceRaw, priceAmount, priceMillions));
    item.setDetails(details);
    item.setDetailsInfo(
        new PropertyDetails(details, areaSqm, rooms, (long) (priceAmount / areaSqm)));
    item.setDiscoveredAt(discoveredAt);
    item.setNew(true);
    item.setViewed(false);
    return item;
  }

  @Test
  @DisplayName("saveCityItems and getItemsByCity persists and retrieves listings")
  void testSaveCityItemsAndGetItemsByCity() {
    IngatlanItem item1 =
        createItem(
            "dh_1",
            "https://dh.hu/1",
            "Duna House",
            "Szeged",
            "Szegedi lakás 1",
            "40 M Ft",
            40_000_000L,
            40.0,
            "50 m2, 2 szoba",
            50.0,
            2.0,
            "2026-03-01 10:00:00");
    IngatlanItem item2 =
        createItem(
            "dh_2",
            "https://dh.hu/2",
            "Duna House",
            "Szeged",
            "Szegedi lakás 2",
            "50 M Ft",
            50_000_000L,
            50.0,
            "60 m2, 2 szoba",
            60.0,
            2.0,
            "2026-03-02 12:00:00");

    storage.saveCityItems("Szeged", List.of(item1, item2));

    List<IngatlanItem> items = storage.getItemsByCity("Szeged");
    assertEquals(2, items.size());
    assertEquals("https://dh.hu/2", items.get(0).getUrl());
    assertEquals("https://dh.hu/1", items.get(1).getUrl());
  }

  @Test
  @DisplayName("saveAll and getAll returns all listings ordered by discoveredAt desc")
  void testSaveAllAndGetAll() {
    IngatlanItem item1 =
        createItem(
            "oc_1",
            "https://oc.hu/1",
            "Otthon Centrum",
            "Budapest",
            "Bp lakás",
            "70 M Ft",
            70_000_000L,
            70.0,
            "70 m2, 3 szoba",
            70.0,
            3.0,
            "2026-03-01 09:00:00");
    IngatlanItem item2 =
        createItem(
            "oc_2",
            "https://oc.hu/2",
            "Otthon Centrum",
            "Debrecen",
            "Debreceni ház",
            "90 M Ft",
            90_000_000L,
            90.0,
            "110 m2, 4 szoba",
            110.0,
            4.0,
            "2026-03-02 11:00:00");

    storage.saveAll(List.of(item1, item2));

    List<IngatlanItem> all = storage.getAll();
    assertEquals(2, all.size());
    assertEquals(2, storage.getTotalCount());
    assertEquals("https://oc.hu/2", all.get(0).getUrl());
  }

  @Test
  @DisplayName("isNewUrl and hasCityDatabase reflect persistence state")
  void testIsNewUrlAndHasCityDatabase() {
    assertFalse(storage.hasCityDatabase("Győr"));
    assertFalse(storage.isNewUrl("Győr", "https://site.hu/gyor1"));
    assertTrue(storage.isNewUrl(null, "https://site.hu/gyor1"));

    IngatlanItem item =
        createItem(
            "p_1",
            "https://site.hu/gyor1",
            "Portal",
            "Győr",
            "Győri lakás",
            "30 M Ft",
            30_000_000L,
            30.0,
            "45 m2, 1 szoba",
            45.0,
            1.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(item));

    assertTrue(storage.hasCityDatabase("Győr"));
    assertFalse(storage.isNewUrl("Győr", "https://site.hu/gyor1"));
    assertTrue(storage.isNewUrl("Győr", "https://site.hu/gyor_another_new"));
    assertTrue(storage.containsUrl("https://site.hu/gyor1"));
  }

  @Test
  @DisplayName("Updating existing listing preserves viewed state")
  void testDuplicateUrlPreservesViewed() {
    String url = "https://dh.hu/view-test";
    IngatlanItem item =
        createItem(
            "dh_v",
            url,
            "Duna House",
            "Budapest",
            "Teszt lakás",
            "50 M Ft",
            50_000_000L,
            50.0,
            "60 m2, 2 szoba",
            60.0,
            2.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(item));

    assertFalse(storage.isViewed(url));
    assertTrue(storage.markAsViewed(url));
    assertTrue(storage.isViewed(url));

    IngatlanItem updated =
        createItem(
            "dh_v",
            url,
            "Duna House",
            "Budapest",
            "Teszt lakás frissítve",
            "50 M Ft",
            50_000_000L,
            50.0,
            "60 m2, 2 szoba",
            60.0,
            2.0,
            "2026-03-01 10:00:00");
    updated.setViewed(false);
    storage.saveAll(List.of(updated));

    assertTrue(storage.isViewed(url));
  }

  @Test
  @DisplayName("Price drop tracking records history, updates delta and status to PRICE_DROP")
  void testPriceDropTracking() {
    String url = "https://dh.hu/price-drop";
    IngatlanItem initial =
        createItem(
            "dh_pd",
            url,
            "Duna House",
            "Szeged",
            "Árcsökkenő ház",
            "60 M Ft",
            60_000_000L,
            60.0,
            "80 m2, 3 szoba",
            80.0,
            3.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(initial));

    List<PriceHistoryEntry> history1 = storage.getPriceHistory(url);
    assertEquals(1, history1.size());
    assertEquals(60_000_000L, history1.get(0).priceAmount());

    IngatlanItem dropped =
        createItem(
            "dh_pd",
            url,
            "Duna House",
            "Szeged",
            "Árcsökkenő ház",
            "54 M Ft",
            54_000_000L,
            54.0,
            "80 m2, 3 szoba",
            80.0,
            3.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(dropped));

    List<PriceHistoryEntry> history2 = storage.getPriceHistory(url);
    assertEquals(2, history2.size());
    assertEquals(-6_000_000L, history2.get(1).priceDelta());
    assertEquals(-10.0, history2.get(1).changePercent());

    List<IngatlanItem> discounted = storage.getDiscountedProperties("Szeged");
    assertEquals(1, discounted.size());
    assertEquals("PRICE_DROP", discounted.get(0).getStatus());
    assertEquals(-6_000_000L, discounted.get(0).getPriceDelta());
  }

  @Test
  @DisplayName("Price increase tracking updates delta and status to PRICE_INCREASE")
  void testPriceIncreaseTracking() {
    String url = "https://oc.hu/price-up";
    IngatlanItem initial =
        createItem(
            "oc_pu",
            url,
            "Otthon Centrum",
            "Pécs",
            "Dráguló lakás",
            "40 M Ft",
            40_000_000L,
            40.0,
            "50 m2, 2 szoba",
            50.0,
            2.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(initial));

    IngatlanItem increased =
        createItem(
            "oc_pu",
            url,
            "Otthon Centrum",
            "Pécs",
            "Dráguló lakás",
            "44 M Ft",
            44_000_000L,
            44.0,
            "50 m2, 2 szoba",
            50.0,
            2.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(increased));

    List<IngatlanItem> items = storage.getItemsByCity("Pécs");
    assertEquals(1, items.size());
    assertEquals("PRICE_INCREASE", items.get(0).getStatus());
    assertEquals(4_000_000L, items.get(0).getPriceDelta());
    assertEquals(10.0, items.get(0).getPriceChangePercent());
  }

  @Test
  @DisplayName("Status log records initial discovery and status transitions")
  void testStatusLogTransitions() {
    String url = "https://dh.hu/status-test";
    IngatlanItem initial =
        createItem(
            "dh_st",
            url,
            "Duna House",
            "Debrecen",
            "Státusz teszt lakás",
            "50 M Ft",
            50_000_000L,
            50.0,
            "60 m2, 2 szoba",
            60.0,
            2.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(initial));

    List<StatusLogEntry> log1 = storage.getStatusLog(url);
    assertEquals(1, log1.size());
    assertEquals("ACTIVE", log1.get(0).status());
    assertEquals("Kezdeti állapot felfedezve", log1.get(0).note());

    IngatlanItem dropped =
        createItem(
            "dh_st",
            url,
            "Duna House",
            "Debrecen",
            "Státusz teszt lakás",
            "45 M Ft",
            45_000_000L,
            45.0,
            "60 m2, 2 szoba",
            60.0,
            2.0,
            "2026-03-01 10:00:00");
    storage.saveAll(List.of(dropped));

    List<StatusLogEntry> log2 = storage.getStatusLog(url);
    assertEquals(2, log2.size());
    assertEquals("PRICE_DROP", log2.get(1).status());
    assertTrue(log2.get(1).note().contains("ACTIVE -> PRICE_DROP"));
  }

  @Test
  @DisplayName("getTrackedCitiesInfo and countByPortal compute aggregation metrics")
  void testTrackedCitiesAndPortals() {
    IngatlanItem i1 =
        createItem(
            "p1",
            "https://dh.hu/a",
            "Duna House",
            "Budapest",
            "A",
            "50 M Ft",
            50_000_000L,
            50.0,
            "50 m2",
            50.0,
            1.0,
            "2026-03-01");
    IngatlanItem i2 =
        createItem(
            "p2",
            "https://dh.hu/b",
            "Duna House",
            "Szeged",
            "B",
            "40 M Ft",
            40_000_000L,
            40.0,
            "40 m2",
            40.0,
            1.0,
            "2026-03-01");
    IngatlanItem i3 =
        createItem(
            "p3",
            "https://oc.hu/c",
            "Otthon Centrum",
            "Budapest",
            "C",
            "60 M Ft",
            60_000_000L,
            60.0,
            "60 m2",
            60.0,
            1.0,
            "2026-03-01");
    storage.saveAll(List.of(i1, i2, i3));

    List<Map<String, Object>> cities = storage.getTrackedCitiesInfo();
    assertEquals(2, cities.size());

    Map<String, Long> portals = storage.countByPortal();
    assertEquals(2L, portals.get("Duna House"));
    assertEquals(1L, portals.get("Otthon Centrum"));

    Set<String> allUrls = storage.getAllUrls();
    assertEquals(3, allUrls.size());
  }

  @Test
  @DisplayName("clearCityHistory and clearHistory remove records")
  void testCleanupOperations() {
    IngatlanItem i1 =
        createItem(
            "c1",
            "https://dh.hu/c1",
            "Duna House",
            "Eger",
            "Egri lakás",
            "30 M Ft",
            30_000_000L,
            30.0,
            "40 m2",
            40.0,
            1.0,
            "2026-03-01");
    IngatlanItem i2 =
        createItem(
            "c2",
            "https://dh.hu/c2",
            "Duna House",
            "Miskolc",
            "Miskolci ház",
            "35 M Ft",
            35_000_000L,
            35.0,
            "50 m2",
            50.0,
            2.0,
            "2026-03-01");
    storage.saveAll(List.of(i1, i2));

    assertEquals(2, storage.getTotalCount());
    storage.clearCityHistory("Eger");
    assertEquals(1, storage.getTotalCount());
    assertFalse(storage.hasCityDatabase("Eger"));
    assertTrue(storage.hasCityDatabase("Miskolc"));

    storage.clearHistory();
    assertEquals(0, storage.getTotalCount());
  }

  @Test
  @DisplayName("findByCriteria filters and paginates through ListingFilterService")
  void testFindByCriteria() {
    IngatlanItem i1 =
        createItem(
            "f1",
            "https://dh.hu/f1",
            "Duna House",
            "Győr",
            "Győri 1",
            "30 M Ft",
            30_000_000L,
            30.0,
            "50 m2, 2 szoba",
            50.0,
            2.0,
            "2026-03-01");
    IngatlanItem i2 =
        createItem(
            "f2",
            "https://dh.hu/f2",
            "Duna House",
            "Győr",
            "Győri 2",
            "70 M Ft",
            70_000_000L,
            70.0,
            "90 m2, 3 szoba",
            90.0,
            3.0,
            "2026-03-02");
    storage.saveAll(List.of(i1, i2));

    ListingFilterDto filter =
        new ListingFilterDto(
            List.of("Győr"),
            null,
            20.0,
            50.0,
            null,
            null,
            null,
            null,
            false,
            false,
            "PRICE_ASC",
            null,
            null);
    List<ListingDto> result = storage.findByCriteria(filter);
    assertEquals(1, result.size());
    assertEquals("https://dh.hu/f1", result.get(0).url());
  }

  @Test
  @DisplayName("ConfigStorage roundtrip for search criteria")
  void testConfigStorage() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Budapest", "Debrecen"));
    criteria.setMaxPriceMillion(80);
    criteria.setMinArea(45);

    storage.saveLastCriteria(criteria);

    SearchCriteria loaded = storage.getLastCriteria();
    assertNotNull(loaded);
    assertEquals(List.of("Budapest", "Debrecen"), loaded.getCities());
    assertEquals(80, loaded.getMaxPriceMillion());
    assertEquals(45, loaded.getMinArea());
  }
}
