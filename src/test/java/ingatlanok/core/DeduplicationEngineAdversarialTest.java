package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SourceListing;

/**
 * Adversarial stress tests for {@link DeduplicationEngine} clustering logic.
 */
class DeduplicationEngineAdversarialTest {

  private final DeduplicationEngine engine = new DeduplicationEngine(null);

  @Test
  @DisplayName("Floor area boundary: exact 5.0% diff matches, 5.1% diff fails to match")
  void testFloorAreaBoundaryTolerance() {
    // 95.0 vs 100.0: diff 5.0 / 100.0 = 0.0500 (exact 5.0%) -> matches
    IngatlanItem item95 = createItem("1", "dh", "http://1", "Lakás 95m2",
        "Budapest", PropertyType.LAKAS, 95.0, 3.0, 50_000_000L, "ACTIVE");
    IngatlanItem item100 = createItem("2", "oc", "http://2", "Lakás 100m2",
        "Budapest", PropertyType.LAKAS, 100.0, 3.0, 50_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(item95, item100), "Exact 5.0% area diff should match");
    List<IngatlanItem> clustered5 = engine.deduplicateMultiSignal(List.of(item95, item100));
    assertEquals(1, clustered5.size());
    assertEquals(2, clustered5.get(0).getSourcesCount());

    // 94.9 vs 100.0: diff 5.1 / 100.0 = 0.051 (5.1%) -> fails
    IngatlanItem item949 = createItem("3", "jf", "http://3", "Lakás 94.9m2",
        "Budapest", PropertyType.LAKAS, 94.9, 3.0, 50_000_000L, "ACTIVE");

    assertFalse(engine.isMatch(item949, item100), "5.1% area diff must fail to match");
    List<IngatlanItem> clustered51 = engine.deduplicateMultiSignal(List.of(item949, item100));
    assertEquals(2, clustered51.size(), "5.1% area diff must result in separate items");
  }

  @Test
  @DisplayName("Room count boundary: 0.5 room diff matches, 1.0 room diff fails conflict guard")
  void testRoomCountBoundaryTolerance() {
    // 3.0 vs 3.5 rooms: diff 0.5 -> matches
    IngatlanItem item3 = createItem("1", "dh", "http://1", "3 szobás",
        "Budapest", PropertyType.LAKAS, 80.0, 3.0, 50_000_000L, "ACTIVE");
    IngatlanItem item35 = createItem("2", "oc", "http://2", "3.5 szobás",
        "Budapest", PropertyType.LAKAS, 80.0, 3.5, 50_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(item3, item35), "0.5 room diff should match");
    List<IngatlanItem> clusteredHalf = engine.deduplicateMultiSignal(List.of(item3, item35));
    assertEquals(1, clusteredHalf.size());

    // 3.0 vs 4.0 rooms: diff 1.0 -> fails conflict guard
    IngatlanItem item4 = createItem("3", "jf", "http://3", "4 szobás",
        "Budapest", PropertyType.LAKAS, 80.0, 4.0, 50_000_000L, "ACTIVE");

    assertFalse(engine.isMatch(item3, item4), "1.0 room diff must fail conflict guard");
    List<IngatlanItem> clusteredOne = engine.deduplicateMultiSignal(List.of(item3, item4));
    assertEquals(2, clusteredOne.size(), "1.0 room diff must not be clustered");
  }

  @Test
  @DisplayName("Price band boundary: exact 20.0% diff matches, 20.1% diff fails to match")
  void testPriceBandBoundaryTolerance() {
    // 100M vs 80M: diff 20M / 100M = 0.200 (exact 20.0%) -> matches
    IngatlanItem item100M = createItem("1", "dh", "http://1", "100M Ft",
        "Budapest", PropertyType.LAKAS, 80.0, 3.0, 100_000_000L, "ACTIVE");
    IngatlanItem item80M = createItem("2", "oc", "http://2", "80M Ft",
        "Budapest", PropertyType.LAKAS, 80.0, 3.0, 80_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(item100M, item80M), "Exact 20.0% price diff should match");
    List<IngatlanItem> clustered20 = engine.deduplicateMultiSignal(List.of(item100M, item80M));
    assertEquals(1, clustered20.size());

    // 100M vs 79.9M: diff 20.1M / 100M = 0.201 (20.1%) -> fails
    IngatlanItem item799M = createItem("3", "jf", "http://3", "79.9M Ft",
        "Budapest", PropertyType.LAKAS, 80.0, 3.0, 79_900_000L, "ACTIVE");

    assertFalse(engine.isMatch(item100M, item799M), "20.1% price diff must fail to match");
    List<IngatlanItem> clustered201 = engine.deduplicateMultiSignal(List.of(item100M, item799M));
    assertEquals(2, clustered201.size(), "20.1% price diff must not be clustered");
  }

  @Test
  @DisplayName("City normalization: accents, whitespace, and mixed case match; different cities do not")
  void testCityNormalizationVariations() {
    // Budapest vs budapest vs BUDAPEST
    IngatlanItem itemMixed = createItem("1", "dh", "http://1", "Budapest Title",
        "Budapest", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    IngatlanItem itemLowerWs = createItem("2", "oc", "http://2", "budapest ws Title",
        "  budapest  ", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    IngatlanItem itemUpper = createItem("3", "jf", "http://3", "BUDAPEST Title",
        "BUDAPEST", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(itemMixed, itemLowerWs));
    assertTrue(engine.isMatch(itemMixed, itemUpper));
    List<IngatlanItem> clusteredBp = engine.deduplicateMultiSignal(List.of(itemMixed, itemLowerWs, itemUpper));
    assertEquals(1, clusteredBp.size());
    assertEquals(3, clusteredBp.get(0).getSourcesCount());

    // Győr vs gyor
    IngatlanItem itemGyorAccent = createItem("4", "dh", "http://4", "Győr",
        "Győr", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    IngatlanItem itemGyorPlain = createItem("5", "oc", "http://5", "gyor",
        "gyor", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(itemGyorAccent, itemGyorPlain));
    List<IngatlanItem> clusteredGyor = engine.deduplicateMultiSignal(List.of(itemGyorAccent, itemGyorPlain));
    assertEquals(1, clusteredGyor.size());

    // Székesfehérvár vs szekesfehervar
    IngatlanItem itemFehervarAccent = createItem("6", "dh", "http://6", "Székesfehérvár",
        "Székesfehérvár", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    IngatlanItem itemFehervarPlain = createItem("7", "oc", "http://7", "szekesfehervar",
        "szekesfehervar", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(itemFehervarAccent, itemFehervarPlain));

    // Different cities: Budapest vs Debrecen
    IngatlanItem itemDeb = createItem("8", "oc", "http://8", "Debrecen",
        "Debrecen", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    assertFalse(engine.isMatch(itemMixed, itemDeb));
  }

  @Test
  @DisplayName("PropertyType matching: LAKAS vs HAZ never cluster, TELEK vs residential never cluster")
  void testPropertyTypeStrictSeparation() {
    IngatlanItem lakas = createItem("1", "dh", "http://1", "Lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 60_000_000L, "ACTIVE");
    IngatlanItem haz = createItem("2", "oc", "http://2", "Ház",
        "Debrecen", PropertyType.HAZ, 100.0, 3.0, 60_000_000L, "ACTIVE");
    IngatlanItem telek = createItem("3", "jf", "http://3", "Telek",
        "Debrecen", PropertyType.TELEK, 100.0, 0.0, 60_000_000L, "ACTIVE");

    assertFalse(engine.isMatch(lakas, haz), "LAKAS and HAZ must never match");
    assertFalse(engine.isMatch(lakas, telek), "LAKAS and TELEK must never match");
    assertFalse(engine.isMatch(haz, telek), "HAZ and TELEK must never match");

    List<IngatlanItem> mixedTypes = engine.deduplicateMultiSignal(List.of(lakas, haz, telek));
    assertEquals(3, mixedTypes.size(), "Different property types must never be clustered together");

    // Non-core types must never cluster even with identical attributes
    IngatlanItem garazs1 = createItem("4", "dh", "http://4", "Garázs 1",
        "Debrecen", PropertyType.GARAZS, 20.0, 0.0, 10_000_000L, "ACTIVE");
    IngatlanItem garazs2 = createItem("5", "oc", "http://5", "Garázs 2",
        "Debrecen", PropertyType.GARAZS, 20.0, 0.0, 10_000_000L, "ACTIVE");

    assertFalse(engine.isMatch(garazs1, garazs2), "GARAZS must not be clustered");
    List<IngatlanItem> garazsList = engine.deduplicateMultiSignal(List.of(garazs1, garazs2));
    assertEquals(2, garazsList.size());
  }

  @Test
  @DisplayName("TELEK room count handling: missing/zero rooms allow clustering, room diff >= 1.0 rejects")
  void testTelekRoomHandling() {
    // Both with 0 rooms -> clusters
    IngatlanItem t1 = createItem("1", "dh", "http://1", "Telek 1",
        "Győr", PropertyType.TELEK, 1000.0, 0.0, 30_000_000L, "ACTIVE");
    IngatlanItem t2 = createItem("2", "oc", "http://2", "Telek 2",
        "Győr", PropertyType.TELEK, 1020.0, 0.0, 29_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(t1, t2));
    assertEquals(1, engine.deduplicateMultiSignal(List.of(t1, t2)).size());

    // One with 0 rooms and one with 1 room -> clusters (0 is treated as missing)
    IngatlanItem t3 = createItem("3", "jf", "http://3", "Telek faházzal",
        "Győr", PropertyType.TELEK, 1000.0, 1.0, 30_000_000L, "ACTIVE");
    assertTrue(engine.isMatch(t1, t3));

    // Both with positive rooms, but diff >= 1.0 -> rejects
    IngatlanItem t4 = createItem("4", "dh", "http://4", "Telek 2 szobás házzal",
        "Győr", PropertyType.TELEK, 1000.0, 2.0, 30_000_000L, "ACTIVE");
    assertFalse(engine.isMatch(t3, t4), "TELEK with room diff >= 1.0 must be rejected");
  }

  @Test
  @DisplayName("Transitive cluster conflict guard: A matches B, B matches C, but A conflicts C -> not clustered in 3")
  void testTransitiveClusterConflictGuard() {
    // A: 95m2, B: 100m2 (diff 5.0% -> matches)
    // B: 100m2, C: 104m2 (diff 3.8% -> matches)
    // A: 95m2 vs C: 104m2 -> diff 9m2 / 104m2 = 8.65% > 5% (A conflicts with C!)
    IngatlanItem itemA = createItem("A", "dh", "http://A", "Lakás A 95m2",
        "Budapest", PropertyType.LAKAS, 95.0, 3.0, 50_000_000L, "ACTIVE");
    IngatlanItem itemB = createItem("B", "oc", "http://B", "Lakás B 100m2",
        "Budapest", PropertyType.LAKAS, 100.0, 3.0, 50_000_000L, "ACTIVE");
    IngatlanItem itemC = createItem("C", "jf", "http://C", "Lakás C 104m2",
        "Budapest", PropertyType.LAKAS, 104.0, 3.0, 50_000_000L, "ACTIVE");

    assertTrue(engine.isMatch(itemA, itemB), "A should match B");
    assertTrue(engine.isMatch(itemB, itemC), "B should match C");
    assertFalse(engine.isMatch(itemA, itemC), "A must conflict with C due to 8.65% area difference");

    // Permutation 1: [A, B, C]
    List<IngatlanItem> result1 = engine.deduplicateMultiSignal(List.of(itemA, itemB, itemC));
    assertEquals(2, result1.size(), "Cluster conflict guard must prevent grouping all 3 together");
    for (IngatlanItem cluster : result1) {
      assertNotEquals(3, cluster.getSourcesCount(), "No cluster may have size 3");
    }

    // Permutation 2: [B, A, C]
    List<IngatlanItem> result2 = engine.deduplicateMultiSignal(List.of(itemB, itemA, itemC));
    assertEquals(2, result2.size());
    for (IngatlanItem cluster : result2) {
      assertNotEquals(3, cluster.getSourcesCount());
    }

    // Permutation 3: [B, C, A]
    List<IngatlanItem> result3 = engine.deduplicateMultiSignal(List.of(itemB, itemC, itemA));
    assertEquals(2, result3.size());
    for (IngatlanItem cluster : result3) {
      assertNotEquals(3, cluster.getSourcesCount());
    }
  }

  @Test
  @DisplayName("Lowest active price selection: cluster with 42M inactive, 50M active, 52M active -> primary is 50M")
  void testLowestActivePriceSelection() {
    // 42M vs 50M: 16.0% diff <= 20%
    // 42M vs 52M: 19.23% diff <= 20%
    // 50M vs 52M: 3.85% diff <= 20%
    // All 3 pairwise match within 20% price band!
    IngatlanItem inactive42M = createItem("1", "dh", "http://dh/42m", "DH Régi Inaktív",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 42_000_000L, "INACTIVE");
    IngatlanItem active50M = createItem("2", "oc", "http://oc/50m", "OC Friss Aktív",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 50_000_000L, "ACTIVE");
    IngatlanItem active52M = createItem("3", "jf", "http://jf/52m", "JF Friss Aktív",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 52_000_000L, "ACTIVE");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(inactive42M, active50M, active52M));
    assertEquals(1, clustered.size(), "All three should be clustered together");

    IngatlanItem primary = clustered.get(0);
    assertEquals(3, primary.getSourcesCount());
    assertEquals(3, primary.getSourceListings().size());

    // Primary card price MUST be 50M HUF (lowest ACTIVE), not 42M HUF (which is INACTIVE)
    assertNotNull(primary.getPriceInfo());
    assertEquals(50_000_000L, primary.getPriceInfo().getAmountHuf(),
        "Primary card price must be lowest ACTIVE price (50M), not inactive (42M)");
    assertEquals("http://oc/50m", primary.getUrl(),
        "Primary card URL must be from the lowest active price source");
    assertEquals("OC Friss Aktív", primary.getTitle());

    // Source listings must be sorted by price ascending: 42M, 50M, 52M
    List<SourceListing> sources = primary.getSourceListings();
    assertEquals(42_000_000L, sources.get(0).getPriceHuf());
    assertEquals("INACTIVE", sources.get(0).getStatus());
    assertEquals(50_000_000L, sources.get(1).getPriceHuf());
    assertEquals("ACTIVE", sources.get(1).getStatus());
    assertEquals(52_000_000L, sources.get(2).getPriceHuf());
    assertEquals("ACTIVE", sources.get(2).getStatus());
  }

  @Test
  @DisplayName("Conflict guard on price: 40M inactive vs 50M active vs 52M active splits into 2 clusters")
  void testPriceConflictGuardSplitsClusterWhenInactiveExceedsBandWithThirdItem() {
    // 40M vs 50M: diff 10M / 50M = 20.0% (matches)
    // 50M vs 52M: diff 2M / 52M = 3.85% (matches)
    // 40M vs 52M: diff 12M / 52M = 23.08% > 20.0% (CONFLICTS!)
    IngatlanItem inactive40M = createItem("1", "dh", "http://dh/40m", "DH Inaktív",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 40_000_000L, "INACTIVE");
    IngatlanItem active50M = createItem("2", "oc", "http://oc/50m", "OC Aktív",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 50_000_000L, "ACTIVE");
    IngatlanItem active52M = createItem("3", "jf", "http://jf/52m", "JF Aktív",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 52_000_000L, "ACTIVE");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(inactive40M, active50M, active52M));
    assertEquals(2, clustered.size(), "40M and 52M conflict (23.08% > 20%), splitting into 2 clusters");

    // In the first cluster [40M, 50M], lowest ACTIVE price is STILL 50M (not 40M inactive)
    IngatlanItem cluster1 = clustered.get(0);
    assertEquals(2, cluster1.getSourcesCount());
    assertEquals(50_000_000L, cluster1.getPriceInfo().getAmountHuf());
    assertEquals("http://oc/50m", cluster1.getUrl());

    // In the second cluster [52M]
    IngatlanItem cluster2 = clustered.get(1);
    assertEquals(1, cluster2.getSourcesCount());
    assertEquals(52_000_000L, cluster2.getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("All inactive listings fallback: selects lowest price among inactive items")
  void testAllInactiveListingsSelectsLowestPrice() {
    IngatlanItem inactive40M = createItem("1", "dh", "http://dh/40m", "DH Inaktív 40",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 40_000_000L, "INACTIVE");
    IngatlanItem inactive45M = createItem("2", "oc", "http://oc/45m", "OC Inaktív 45",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 45_000_000L, "INACTIVE");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(inactive40M, inactive45M));
    assertEquals(1, clustered.size());
    IngatlanItem primary = clustered.get(0);
    assertEquals(40_000_000L, primary.getPriceInfo().getAmountHuf());
    assertEquals("http://dh/40m", primary.getUrl());
  }

  @Test
  @DisplayName("Image fallback: primary adopts image from another cluster member if primary has no image")
  void testImageAdoptionFallback() {
    IngatlanItem activeNoImage = createItem("1", "dh", "http://dh/noimg", "Kép nélkül",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 50_000_000L, "ACTIVE");
    activeNoImage.setImageUrl(null);

    IngatlanItem activeWithImage = createItem("2", "oc", "http://oc/withimg", "Képpel",
        "Debrecen", PropertyType.LAKAS, 70.0, 2.0, 52_000_000L, "ACTIVE");
    activeWithImage.setImageUrl("https://images.example.com/photo.jpg");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(activeNoImage, activeWithImage));
    assertEquals(1, clustered.size());
    IngatlanItem primary = clustered.get(0);
    assertEquals("https://images.example.com/photo.jpg", primary.getImageUrl());
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
