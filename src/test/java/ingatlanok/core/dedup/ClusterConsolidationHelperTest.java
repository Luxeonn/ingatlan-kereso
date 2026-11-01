package ingatlanok.core.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;

class ClusterConsolidationHelperTest {

  @Test
  @DisplayName("hasPositiveArea és hasPositiveRooms helyes értéket ad null és pozitív értékekre")
  void testHasPositiveAreaAndRooms() {
    assertFalse(ClusterConsolidationHelper.hasPositiveArea(null));
    assertFalse(ClusterConsolidationHelper.hasPositiveRooms(null));

    IngatlanItem item = new IngatlanItem();
    assertFalse(ClusterConsolidationHelper.hasPositiveArea(item));
    assertFalse(ClusterConsolidationHelper.hasPositiveRooms(item));

    item.setDetailsInfo(PropertyDetails.builder().areaSquareMeters(0.0).roomCount(0.0).build());
    assertFalse(ClusterConsolidationHelper.hasPositiveArea(item));
    assertFalse(ClusterConsolidationHelper.hasPositiveRooms(item));

    item.setDetailsInfo(PropertyDetails.builder().areaSquareMeters(50.0).roomCount(2.0).build());
    assertTrue(ClusterConsolidationHelper.hasPositiveArea(item));
    assertTrue(ClusterConsolidationHelper.hasPositiveRooms(item));
  }

  @Test
  @DisplayName("adoptMissingDetailsFromCluster átveszi a hiányzó alapterületet és szobaszámot a klaszterből")
  void testAdoptMissingDetailsFromCluster() {
    IngatlanItem primary = new IngatlanItem();
    primary.setPriceInfo(PriceInfo.builder().amountHuf(30_000_000L).build());

    IngatlanItem memberWithArea = new IngatlanItem();
    memberWithArea.setDetailsInfo(PropertyDetails.builder().areaSquareMeters(60.0).build());

    IngatlanItem memberWithRooms = new IngatlanItem();
    memberWithRooms.setDetailsInfo(PropertyDetails.builder().roomCount(3.0).build());

    ClusterConsolidationHelper.adoptMissingDetailsFromCluster(
        primary, List.of(primary, memberWithArea, memberWithRooms));

    assertNotNull(primary.getDetailsInfo());
    assertEquals(60.0, primary.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, primary.getDetailsInfo().getRoomCount());
    assertEquals(500_000L, primary.getDetailsInfo().getPricePerSquareMeter());
    assertEquals("60 m² • 3 szoba", primary.getDetailsInfo().getRawText());
    assertEquals("60 m² • 3 szoba", primary.getDetails());
  }

  @Test
  @DisplayName("adoptMissingDetailsFromCluster null és üres bemenetek esetén hibamentes")
  void testAdoptMissingDetailsEdgeCases() {
    ClusterConsolidationHelper.adoptMissingDetailsFromCluster(null, null);
    ClusterConsolidationHelper.adoptMissingDetailsFromCluster(new IngatlanItem(), List.of());
  }

  @Test
  @DisplayName("resolveSourcePricePerM2 kiszámítja az m² árat meglévő adatból vagy fallbackből")
  void testResolveSourcePricePerM2() {
    IngatlanItem itemWithSqm = new IngatlanItem();
    itemWithSqm.setDetailsInfo(PropertyDetails.builder().pricePerSquareMeter(700_000L).build());
    assertEquals(700_000L, ClusterConsolidationHelper.resolveSourcePricePerM2(itemWithSqm, 35_000_000L, 50.0));

    IngatlanItem itemWithoutSqm = new IngatlanItem();
    assertEquals(500_000L, ClusterConsolidationHelper.resolveSourcePricePerM2(itemWithoutSqm, 25_000_000L, 50.0));

    assertNull(ClusterConsolidationHelper.resolveSourcePricePerM2(itemWithoutSqm, null, 50.0));
    assertNull(ClusterConsolidationHelper.resolveSourcePricePerM2(itemWithoutSqm, 25_000_000L, null));
  }

  @Test
  @DisplayName("hasMatchingImage felismeri az azonos képeket query paraméterek és protokoll nélkül")
  void testHasMatchingImage() {
    assertFalse(ClusterConsolidationHelper.hasMatchingImage(null, null));

    IngatlanItem a = new IngatlanItem();
    IngatlanItem b = new IngatlanItem();
    assertFalse(ClusterConsolidationHelper.hasMatchingImage(a, b));

    a.setImageUrl("https://cdn.example.com/photo1.jpg?w=800");
    b.setImageUrl("http://cdn.example.com/photo1.jpg?v=2");
    assertTrue(ClusterConsolidationHelper.hasMatchingImage(a, b));

    b.setImageUrl("https://cdn.example.com/photo2.jpg");
    assertFalse(ClusterConsolidationHelper.hasMatchingImage(a, b));

    // Placeholder képek kizárása
    a.setImageUrl("https://example.com/no-image.png");
    b.setImageUrl("https://example.com/no-image.png");
    assertFalse(ClusterConsolidationHelper.hasMatchingImage(a, b));
  }

  @Test
  @DisplayName("normalizeImageUrl kezeli a null, üres és placeholder értékeket")
  void testNormalizeImageUrl() {
    assertEquals("", ClusterConsolidationHelper.normalizeImageUrl(null));
    assertEquals("", ClusterConsolidationHelper.normalizeImageUrl(""));
    assertEquals("", ClusterConsolidationHelper.normalizeImageUrl("   "));
    assertEquals("", ClusterConsolidationHelper.normalizeImageUrl("https://img.hu/default.jpg"));
    assertEquals("img.hu/pic.jpg", ClusterConsolidationHelper.normalizeImageUrl("https://img.hu/pic.jpg?x=1"));
  }
}
