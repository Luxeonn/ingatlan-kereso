package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.persistence.ListingStorage;

class DeduplicationEngineTest {

  @Test
  @DisplayName("Deduplikáció URL alapján a sorrend megőrzésével")
  void testDeduplicateByUrl() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem item1 =
        new IngatlanItem(
            "1", "dh", "http://1", "Első", "50 M Ft", "Debrecen", "", "2026-10-14", false);
    IngatlanItem item2 =
        new IngatlanItem(
            "2", "dh", "http://2", "Második", "60 M Ft", "Debrecen", "", "2026-10-14", false);
    IngatlanItem item3 =
        new IngatlanItem(
            "3",
            "dh",
            "http://1",
            "Első duplikátum",
            "50 M Ft",
            "Debrecen",
            "",
            "2026-10-14",
            false);

    List<IngatlanItem> deduped = engine.deduplicateByUrl(List.of(item1, item2, item3));
    assertEquals(2, deduped.size());
    assertEquals("http://1", deduped.get(0).getUrl());
    assertEquals("Első", deduped.get(0).getTitle());
    assertEquals("http://2", deduped.get(1).getUrl());
  }

  @Test
  @DisplayName("Baseline snapshot esetén minden találat isNew = false értéket kap")
  void testApplyNewStatusBaseline() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem item =
        new IngatlanItem(
            "1", "dh", "http://1", "Új városi", "50 M Ft", "Eger", "", "2026-10-14", true);
    engine.applyNewStatus("Eger", List.of(item), true);

    assertFalse(item.isNew());
  }

  @Test
  @DisplayName("Ismert város esetén a tároló alapján kap új státuszt")
  void testApplyNewStatusKnownCity() {
    ListingStorage mockStorage =
        new ListingStorage() {
          @Override
          public void saveCityItems(String city, Collection<IngatlanItem> items) {}

          @Override
          public void saveAll(Collection<IngatlanItem> items) {}

          @Override
          public List<IngatlanItem> getItemsByCity(String city) {
            return List.of();
          }

          @Override
          public List<IngatlanItem> getAll() {
            return List.of();
          }

          @Override
          public boolean hasCityDatabase(String city) {
            return true;
          }

          @Override
          public boolean isNewUrl(String city, String url) {
            return "http://new".equals(url);
          }

          @Override
          public boolean containsUrl(String url) {
            return false;
          }

          @Override
          public boolean markAsViewed(String url) {
            return false;
          }

          @Override
          public boolean isViewed(String url) {
            return false;
          }

          @Override
          public Set<String> getAllUrls() {
            return Set.of();
          }

          @Override
          public List<Map<String, Object>> getTrackedCitiesInfo() {
            return List.of();
          }

          @Override
          public void clearCityHistory(String city) {}

          @Override
          public void clearHistory() {}

          @Override
          public int getTotalCount() {
            return 0;
          }

          @Override
          public String getLastRunTime() {
            return null;
          }
        };

    DeduplicationEngine engine = new DeduplicationEngine(mockStorage);
    IngatlanItem item1 =
        new IngatlanItem(
            "1", "dh", "http://new", "Új tétel", "50 M Ft", "Debrecen", "", "2026-10-14", false);
    IngatlanItem item2 =
        new IngatlanItem(
            "2", "dh", "http://old", "Régi tétel", "50 M Ft", "Debrecen", "", "2026-10-14", false);

    engine.applyNewStatus("Debrecen", List.of(item1, item2), false);

    assertTrue(item1.isNew());
    assertFalse(item2.isNew());
  }

  @Test
  @DisplayName("Kereszt-portál klaszterezés azonos fizikai ingatlan esetén és legolcsóbb forrás kiemelése")
  void testMultiPortalClusteringMatchesAndLowestPriceSelected() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem dh = createItem("dh-1", "Duna House", "http://dh/1", "DH Családi Lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem oc = createItem("oc-2", "Otthon Centrum", "http://oc/2", "OC Belvárosi Lakás",
        "Debrecen", PropertyType.LAKAS, 104.0, 3.0, 75_000_000L, "ACTIVE");
    IngatlanItem jf = createItem("jf-3", "Jófogás", "http://jf/3", "Jófogás 3 szobás",
        "Debrecen", PropertyType.LAKAS, 102.0, 3.5, 78_000_000L, "ACTIVE");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(dh, oc, jf));

    assertEquals(1, clustered.size());
    IngatlanItem primary = clustered.get(0);
    assertEquals(3, primary.getSourcesCount());
    assertEquals(3, primary.getSourceListings().size());
    // Lowest active price is 75M from Otthon Centrum
    assertEquals("http://oc/2", primary.getUrl());
    assertEquals("OC Belvárosi Lakás", primary.getTitle());
    assertEquals(75_000_000L, primary.getPriceInfo().getAmountHuf());
    // Sources sorted by price ascending: 75M, 78M, 80M
    assertEquals(75_000_000L, primary.getSourceListings().get(0).getPriceHuf());
    assertEquals(78_000_000L, primary.getSourceListings().get(1).getPriceHuf());
    assertEquals(80_000_000L, primary.getSourceListings().get(2).getPriceHuf());
  }

  @Test
  @DisplayName("Alapterület tolerancia határ: <=5% csoportosítva, >5% elutasítva")
  void testAreaToleranceBoundary() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    // 100m2 és 105m2: diff 5, max 105 -> 5/105 = 0.0476 <= 0.05 -> klaszterezve
    IngatlanItem itemA = createItem("1", "dh", "http://1", "Lakás A",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem itemB = createItem("2", "oc", "http://2", "Lakás B",
        "Debrecen", PropertyType.LAKAS, 105.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> resultWithin5 = engine.deduplicateMultiSignal(List.of(itemA, itemB));
    assertEquals(1, resultWithin5.size());
    assertEquals(2, resultWithin5.get(0).getSourcesCount());

    // 100m2 és 106m2: diff 6, max 106 -> 6/106 = 0.0566 > 0.05 -> konfliktus védelem
    IngatlanItem itemC = createItem("3", "oc", "http://3", "Lakás C",
        "Debrecen", PropertyType.LAKAS, 106.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> resultOver5 = engine.deduplicateMultiSignal(List.of(itemA, itemC));
    assertEquals(2, resultOver5.size());
  }

  @Test
  @DisplayName("Szobaszám konfliktusvédelem: >=1 szoba eltérés esetén nem vonható össze")
  void testRoomCountConflictGuard() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    // 3 szoba vs 4 szoba -> diff >= 1.0 -> elutasítva
    IngatlanItem item3 = createItem("1", "dh", "http://1", "3 szobás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem item4 = createItem("2", "oc", "http://2", "4 szobás",
        "Debrecen", PropertyType.LAKAS, 100.0, 4.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> resultDiff1 = engine.deduplicateMultiSignal(List.of(item3, item4));
    assertEquals(2, resultDiff1.size());

    // 3 szoba vs 3.5 szoba -> diff 0.5 <= 0.5 -> engedélyezve
    IngatlanItem item35 = createItem("3", "jf", "http://3", "3 és fél szobás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.5, 80_000_000L, "ACTIVE");

    List<IngatlanItem> resultDiffHalf = engine.deduplicateMultiSignal(List.of(item3, item35));
    assertEquals(1, resultDiffHalf.size());
  }

  @Test
  @DisplayName("Ársáv érvényesség: <=20% eltérés megengedett, >20% eltérés elutasítva")
  void testPriceBandSanity() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    // 100M vs 82M: diff 18M / 100M = 0.18 <= 0.20 -> csoportosítva
    IngatlanItem item100 = createItem("1", "dh", "http://1", "Lakás 100M",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 100_000_000L, "ACTIVE");
    IngatlanItem item82 = createItem("2", "oc", "http://2", "Lakás 82M",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 82_000_000L, "ACTIVE");

    List<IngatlanItem> resultWithin20 = engine.deduplicateMultiSignal(List.of(item100, item82));
    assertEquals(1, resultWithin20.size());

    // 100M vs 75M: diff 25M / 100M = 0.25 > 0.20 -> külön marad
    IngatlanItem item75 = createItem("3", "jf", "http://3", "Lakás 75M",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 75_000_000L, "ACTIVE");

    List<IngatlanItem> resultOver20 = engine.deduplicateMultiSignal(List.of(item100, item75));
    assertEquals(2, resultOver20.size());
  }

  @Test
  @DisplayName("Eltérő kanonikus típusok és nem alaptípusok (EGYEB_ISMERETLEN) nem vonhatók össze")
  void testPropertyTypeRejection() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem lakas = createItem("1", "dh", "http://1", "Lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem haz = createItem("2", "oc", "http://2", "Ház",
        "Debrecen", PropertyType.HAZ, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> diffTypes = engine.deduplicateMultiSignal(List.of(lakas, haz));
    assertEquals(2, diffTypes.size());

    // EGYEB_ISMERETLEN nem képez klasztert
    IngatlanItem other1 = createItem("3", "dh", "http://3", "Egyéb 1",
        "Debrecen", PropertyType.EGYEB_ISMERETLEN, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem other2 = createItem("4", "oc", "http://4", "Egyéb 2",
        "Debrecen", PropertyType.EGYEB_ISMERETLEN, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> unknownTypes = engine.deduplicateMultiSignal(List.of(other1, other2));
    assertEquals(2, unknownTypes.size());
  }

  @Test
  @DisplayName("TELEK típusok összevonása alapterület és ár alapján")
  void testTelekGrouping() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem telek1 = createItem("1", "dh", "http://t1", "Építési telek",
        "Debrecen", PropertyType.TELEK, 800.0, 0.0, 25_000_000L, "ACTIVE");
    IngatlanItem telek2 = createItem("2", "oc", "http://t2", "Kiváló telek",
        "Debrecen", PropertyType.TELEK, 820.0, 0.0, 24_000_000L, "ACTIVE");

    List<IngatlanItem> clusteredTelek = engine.deduplicateMultiSignal(List.of(telek1, telek2));
    assertEquals(1, clusteredTelek.size());
    assertEquals(2, clusteredTelek.get(0).getSourcesCount());
    assertEquals(24_000_000L, clusteredTelek.get(0).getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Eltérő városok nem vonhatók össze, de kis- és nagybetű normalizálás működik")
  void testCityNormalizationAndSeparation() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem deb = createItem("1", "dh", "http://1", "Debreceni lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem eger = createItem("2", "oc", "http://2", "Egri lakás",
        "Eger", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> diffCity = engine.deduplicateMultiSignal(List.of(deb, eger));
    assertEquals(2, diffCity.size());

    IngatlanItem debLower = createItem("3", "oc", "http://3", "debreceni kisbetűs",
        "debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> sameCityNormalized = engine.deduplicateMultiSignal(List.of(deb, debLower));
    assertEquals(1, sameCityNormalized.size());
    assertEquals(2, sameCityNormalized.get(0).getSourcesCount());
  }

  @Test
  @DisplayName("Inaktív forrás helyett az aktív legalacsonyabb árú forrás válik elsődlegessé")
  void testLowestActivePriceSelectedOverInactive() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem dhInactive = createItem("1", "dh", "http://dh/inactive", "DH Inaktív Olcsó",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 65_000_000L, "INACTIVE");
    IngatlanItem ocActive = createItem("2", "oc", "http://oc/active", "OC Aktív Reális",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 75_000_000L, "ACTIVE");
    IngatlanItem jfActive = createItem("3", "jf", "http://jf/active", "JF Aktív Drágább",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(dhInactive, ocActive, jfActive));
    assertEquals(1, clustered.size());
    IngatlanItem primary = clustered.get(0);
    assertEquals("http://oc/active", primary.getUrl());
    assertEquals(75_000_000L, primary.getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Egyedi forrású hirdetés sourcesCount = 1 és pontosan 1 sourceListing elemet kap")
  void testSingleSourceListingHandling() {
    DeduplicationEngine engine = new DeduplicationEngine(null);

    IngatlanItem single = createItem("1", "dh", "http://single", "Egyedi hirdetés",
        "Debrecen", PropertyType.LAKAS, 60.0, 2.0, 50_000_000L, "ACTIVE");

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(single));
    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getSourcesCount());
    assertEquals(1, result.get(0).getSourceListings().size());
    assertEquals("http://single", result.get(0).getSourceListings().get(0).getUrl());
  }

  @Test
  @DisplayName("Null és üres kollekciók kezelése")
  void testNullAndEmptyHandling() {
    DeduplicationEngine engine = new DeduplicationEngine(null);
    assertTrue(engine.deduplicateMultiSignal(null).isEmpty());
    assertTrue(engine.deduplicateMultiSignal(List.of()).isEmpty());
    assertEquals(0, engine.deduplicate(List.of()).size());
  }

  private IngatlanItem createItem(
      String id,
      String portal,
      String url,
      String title,
      String city,
      PropertyType type,
      double area,
      double rooms,
      long priceHuf,
      String status) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setPortal(portal);
    item.setUrl(url);
    item.setTitle(title);
    item.setCity(city);
    item.setLocation(city);
    item.setPropertyType(type);
    item.setDetailsInfo(
        new PropertyDetails(
            area + " m2, " + rooms + " szoba",
            area,
            rooms,
            Math.round((double) priceHuf / area)));
    item.setPriceInfo(
        new PriceInfo((priceHuf / 1_000_000.0) + " M Ft", priceHuf, priceHuf / 1_000_000.0));
    item.setPrice((priceHuf / 1_000_000.0) + " M Ft");
    item.setStatus(status);
    return item;
  }
}
