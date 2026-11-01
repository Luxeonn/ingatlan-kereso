package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.MarketSegmentStats;
import ingatlanok.model.MarketStats;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.presentation.MarketSegmentStatsDto;
import ingatlanok.presentation.MarketStatsDto;
import ingatlanok.presentation.PortalStatusDto;
import ingatlanok.presentation.PriceDropItemDto;

/**
 * Empirical stress test harness challenging MarketAnalyticsEngine segment calculations,
 * mathematical median accuracy, bargain count consistency, and DTO immutability.
 */
class MarketAnalyticsEmpiricalStressTest {

  private MarketAnalyticsEngine engine;

  @BeforeEach
  void setUp() {
    engine = new MarketAnalyticsEngine();
  }

  private IngatlanItem createItem(
      String id,
      String city,
      PropertyType type,
      long amountHuf,
      double areaM2,
      Long pricePerM2) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setUrl("https://example.com/property/" + id);
    item.setTitle("Property " + id);
    item.setCity(city);
    item.setLocation(city);
    item.setPropertyType(type);
    item.setPortal("dh");
    item.setPriceInfo(new PriceInfo(amountHuf / 1_000_000.0 + " M Ft", amountHuf, amountHuf / 1_000_000.0));
    item.setDetailsInfo(new PropertyDetails(areaM2 + " m²", areaM2, 2.0, pricePerM2));
    item.setQualityScore(90);
    return item;
  }

  @Test
  @DisplayName("Empirical Median: Comprehensive odd and even sample size mathematical accuracy")
  void testComprehensiveMathematicalMedians() {
    // 1 element
    assertEquals(500L, engine.calculateMedian(List.of(500L)));

    // 2 elements: (100 + 200) / 2 = 150
    assertEquals(150L, engine.calculateMedian(List.of(200L, 100L)));

    // 3 elements: middle is 200
    assertEquals(200L, engine.calculateMedian(List.of(300L, 100L, 200L)));

    // 4 elements: (200 + 300) / 2 = 250
    assertEquals(250L, engine.calculateMedian(List.of(400L, 100L, 300L, 200L)));

    // 5 elements: middle is 300
    assertEquals(300L, engine.calculateMedian(List.of(500L, 100L, 400L, 200L, 300L)));

    // 6 elements with whole average: (300 + 400) / 2 = 350
    assertEquals(350L, engine.calculateMedian(List.of(600L, 100L, 500L, 200L, 400L, 300L)));

    // 6 elements with rounding: (300 + 301) / 2.0 = 300.5 -> rounds to 301
    assertEquals(301L, engine.calculateMedian(List.of(600L, 100L, 500L, 200L, 301L, 300L)));

    // 7 elements: middle is 400
    assertEquals(400L, engine.calculateMedian(List.of(700L, 100L, 600L, 200L, 500L, 300L, 400L)));

    // 8 elements: (400 + 500) / 2 = 450
    assertEquals(450L, engine.calculateMedian(List.of(800L, 100L, 700L, 200L, 600L, 300L, 500L, 400L)));

    // 9 elements: middle is 50
    assertEquals(50L, engine.calculateMedian(List.of(90L, 10L, 80L, 20L, 70L, 30L, 60L, 40L, 50L)));

    // 10 elements with rounding: middle (50, 55) -> 52.5 -> 53
    assertEquals(53L, engine.calculateMedian(List.of(100L, 10L, 90L, 20L, 80L, 30L, 70L, 40L, 55L, 50L)));

    // Null and empty
    assertNull(engine.calculateMedian(null));
    assertNull(engine.calculateMedian(List.of()));
  }

  @Test
  @DisplayName("Empirical Double Median: Accuracy and 1 decimal place rounding")
  void testComprehensiveDoubleMedians() {
    // Odd
    assertEquals(20.3, engine.calculateDoubleMedian(List.of(30.5, 10.1, 20.3)));

    // Even
    assertEquals(25.0, engine.calculateDoubleMedian(List.of(40.0, 10.0, 30.0, 20.0)));

    // Even with rounding: (20.25 + 30.35) / 2.0 = 25.3
    assertEquals(25.3, engine.calculateDoubleMedian(List.of(40.0, 10.0, 30.35, 20.25)));

    // Null and empty
    assertNull(engine.calculateDoubleMedian(null));
    assertNull(engine.calculateDoubleMedian(List.of()));
  }

  @Test
  @DisplayName("Empirical Multi-Segment: Accurate segment breakdown and bargain count invariants")
  void testMultiSegmentBreakdownAndBargainCounts() {
    List<IngatlanItem> items = new ArrayList<>();

    // Segment 1: Budapest LAKAS (6 items, median = 1,000,000 Ft/m2)
    // Items: 750k (bargain: 25%), 1M, 1M, 1M, 1.2M, 1.4M
    items.add(createItem("bp_l1", "Budapest", PropertyType.LAKAS, 37_500_000L, 50.0, 750_000L));
    items.add(createItem("bp_l2", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L));
    items.add(createItem("bp_l3", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L));
    items.add(createItem("bp_l4", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L));
    items.add(createItem("bp_l5", "Budapest", PropertyType.LAKAS, 60_000_000L, 50.0, 1_200_000L));
    items.add(createItem("bp_l6", "Budapest", PropertyType.LAKAS, 70_000_000L, 50.0, 1_400_000L));

    // Segment 2: Budapest HAZ (5 items, median = 800,000 Ft/m2)
    // Items: 600k (bargain: 25%), 700k, 800k, 900k, 1M
    items.add(createItem("bp_h1", "Budapest", PropertyType.HAZ, 60_000_000L, 100.0, 600_000L));
    items.add(createItem("bp_h2", "Budapest", PropertyType.HAZ, 70_000_000L, 100.0, 700_000L));
    items.add(createItem("bp_h3", "Budapest", PropertyType.HAZ, 80_000_000L, 100.0, 800_000L));
    items.add(createItem("bp_h4", "Budapest", PropertyType.HAZ, 90_000_000L, 100.0, 900_000L));
    items.add(createItem("bp_h5", "Budapest", PropertyType.HAZ, 100_000_000L, 100.0, 1_000_000L));

    // Segment 3: Debrecen LAKAS (5 items, median = 700,000 Ft/m2)
    // Items: 600k (14.3% discount -> not bargain), 650k, 700k, 750k, 800k
    items.add(createItem("deb_l1", "Debrecen", PropertyType.LAKAS, 30_000_000L, 50.0, 600_000L));
    items.add(createItem("deb_l2", "Debrecen", PropertyType.LAKAS, 32_500_000L, 50.0, 650_000L));
    items.add(createItem("deb_l3", "Debrecen", PropertyType.LAKAS, 35_000_000L, 50.0, 700_000L));
    items.add(createItem("deb_l4", "Debrecen", PropertyType.LAKAS, 37_500_000L, 50.0, 750_000L));
    items.add(createItem("deb_l5", "Debrecen", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L));

    // Segment 4: Szeged TELEK (5 items, median = 30,000 Ft/m2)
    // Items: 20k (bargain: 33.3%), 25k, 30k, 35k, 40k
    items.add(createItem("szeg_t1", "Szeged", PropertyType.TELEK, 20_000_000L, 1000.0, 20_000L));
    items.add(createItem("szeg_t2", "Szeged", PropertyType.TELEK, 25_000_000L, 1000.0, 25_000L));
    items.add(createItem("szeg_t3", "Szeged", PropertyType.TELEK, 30_000_000L, 1000.0, 30_000L));
    items.add(createItem("szeg_t4", "Szeged", PropertyType.TELEK, 35_000_000L, 1000.0, 35_000L));
    items.add(createItem("szeg_t5", "Szeged", PropertyType.TELEK, 40_000_000L, 1000.0, 40_000L));

    // Segment 5: Pecs GARAZS (4 items, sample size < 5 -> insufficient data)
    // Even though 50k is heavily discounted, bargain MUST BE suppressed!
    items.add(createItem("pecs_g1", "Pecs", PropertyType.GARAZS, 1_000_000L, 20.0, 50_000L));
    items.add(createItem("pecs_g2", "Pecs", PropertyType.GARAZS, 3_000_000L, 20.0, 150_000L));
    items.add(createItem("pecs_g3", "Pecs", PropertyType.GARAZS, 4_000_000L, 20.0, 200_000L));
    items.add(createItem("pecs_g4", "Pecs", PropertyType.GARAZS, 5_000_000L, 20.0, 250_000L));

    MarketStats stats = engine.calculateMarketStats(items);
    assertNotNull(stats);
    assertEquals(5, stats.getSegments().size());

    // Validate Budapest LAKAS
    MarketSegmentStats bpLakas = stats.getSegments().stream()
        .filter(s -> "Budapest".equals(s.getCity()) && PropertyType.LAKAS == s.getPropertyType())
        .findFirst().orElseThrow();
    assertEquals(6, bpLakas.getSampleSize());
    assertFalse(bpLakas.isInsufficientData());
    assertEquals(1_000_000L, bpLakas.getMedianPricePerM2());
    assertEquals(1, bpLakas.getBargainCount());

    // Validate Budapest HAZ
    MarketSegmentStats bpHaz = stats.getSegments().stream()
        .filter(s -> "Budapest".equals(s.getCity()) && PropertyType.HAZ == s.getPropertyType())
        .findFirst().orElseThrow();
    assertEquals(5, bpHaz.getSampleSize());
    assertFalse(bpHaz.isInsufficientData());
    assertEquals(800_000L, bpHaz.getMedianPricePerM2());
    assertEquals(1, bpHaz.getBargainCount());

    // Validate Debrecen LAKAS
    MarketSegmentStats debLakas = stats.getSegments().stream()
        .filter(s -> "Debrecen".equals(s.getCity()) && PropertyType.LAKAS == s.getPropertyType())
        .findFirst().orElseThrow();
    assertEquals(5, debLakas.getSampleSize());
    assertFalse(debLakas.isInsufficientData());
    assertEquals(700_000L, debLakas.getMedianPricePerM2());
    assertEquals(0, debLakas.getBargainCount());

    // Validate Szeged TELEK
    MarketSegmentStats szegTelek = stats.getSegments().stream()
        .filter(s -> "Szeged".equals(s.getCity()) && PropertyType.TELEK == s.getPropertyType())
        .findFirst().orElseThrow();
    assertEquals(5, szegTelek.getSampleSize());
    assertFalse(szegTelek.isInsufficientData());
    assertEquals(30_000L, szegTelek.getMedianPricePerM2());
    assertEquals(1, szegTelek.getBargainCount());

    // Validate Pecs GARAZS (insufficient data)
    MarketSegmentStats pecsGarazs = stats.getSegments().stream()
        .filter(s -> "Pecs".equals(s.getCity()) && PropertyType.GARAZS == s.getPropertyType())
        .findFirst().orElseThrow();
    assertEquals(4, pecsGarazs.getSampleSize());
    assertTrue(pecsGarazs.isInsufficientData());
    assertNull(pecsGarazs.getMedianPricePerM2());
    assertEquals(0, pecsGarazs.getBargainCount());

    // Core invariant: sum of segment bargains == total bargains
    int sumSegmentBargains = stats.getSegments().stream()
        .mapToInt(MarketSegmentStats::getBargainCount)
        .sum();
    assertEquals(3, stats.getBargainCount());
    assertEquals(3, sumSegmentBargains);

    // Total items: 6 + 5 + 5 + 5 + 4 = 25
    // Bargain rate: 3 / 25 = 12.0%
    assertEquals(12.0, stats.getBargainRatePercent());
  }

  @Test
  @DisplayName("Empirical Immutability: MarketSegmentStatsDto has zero setters and toBuilder() copies immutably")
  void testMarketSegmentStatsDtoImmutability() {
    // 1. Reflection check: zero methods starting with "set"
    List<Method> setterMethods = Arrays.stream(MarketSegmentStatsDto.class.getDeclaredMethods())
        .filter(m -> m.getName().startsWith("set"))
        .toList();
    assertTrue(setterMethods.isEmpty(),
        "MarketSegmentStatsDto must not declare any setter methods, but found: " + setterMethods);

    // 2. Reflection check across other read-only DTOs
    Class<?>[] readOnlyClasses = {
        MarketStatsDto.class,
        MarketSegmentStatsDto.class,
        PriceDropItemDto.class,
        PortalStatusDto.class,
        DashboardResponseDto.class,
        PortalSource.class
    };

    for (Class<?> clazz : readOnlyClasses) {
      List<Method> setters = Arrays.stream(clazz.getDeclaredMethods())
          .filter(m -> m.getName().startsWith("set") && !Modifier.isStatic(m.getModifiers()))
          .toList();
      assertTrue(setters.isEmpty(),
          clazz.getSimpleName() + " must have 0 non-static setters, but found: " + setters);
    }

    // 3. toBuilder() immutability verification
    MarketSegmentStatsDto original = MarketSegmentStatsDto.builder()
        .city("Debrecen")
        .propertyType(PropertyType.LAKAS)
        .propertyTypeName("Lakás")
        .residential(true)
        .plot(false)
        .sampleSize(10)
        .insufficientData(false)
        .medianPricePerM2(800_000L)
        .averagePricePerM2(820_000L)
        .medianPriceMillion(45.0)
        .averagePriceMillion(46.0)
        .minPriceMillion(30.0)
        .maxPriceMillion(60.0)
        .bargainCount(0)
        .build();

    MarketSegmentStatsDto modified = original.toBuilder()
        .bargainCount(5)
        .build();

    assertNotEquals(original, modified);
    assertEquals(0, original.getBargainCount(), "Original instance bargainCount must remain 0");
    assertEquals(5, modified.getBargainCount(), "Modified instance bargainCount must be 5");
    assertEquals(original.getCity(), modified.getCity());
    assertEquals(original.getPropertyType(), modified.getPropertyType());
    assertEquals(original.getMedianPricePerM2(), modified.getMedianPricePerM2());
    assertEquals(original.getSampleSize(), modified.getSampleSize());
  }

  @Test
  @DisplayName("Empirical Boundary: 20.00% is bargain, 19.99% is not bargain")
  void testExactBargainThresholdBoundary() {
    List<IngatlanItem> items = new ArrayList<>();
    // 5 base items with median 1,000,000 Ft/m2
    for (int i = 1; i <= 5; i++) {
      items.add(createItem("base" + i, "Gyor", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L));
    }

    // Candidate A: 800,000 -> 20.0% below 1,000,000 -> bargain!
    IngatlanItem item20 = createItem("c20", "Gyor", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L);

    // Candidate B: 800,100 -> 19.99% below 1,000,000 -> NOT bargain!
    IngatlanItem item1999 = createItem("c1999", "Gyor", PropertyType.LAKAS, 40_005_000L, 50.0, 800_100L);

    items.add(item20);
    items.add(item1999);

    MarketStats stats = engine.calculateMarketStats(items);
    assertNotNull(stats);

    assertTrue(item20.isBargain(), "800,000 vs 1M (20.0% below) must be a bargain");
    assertEquals(20.0, item20.getPercentageBelowMedian());

    assertFalse(item1999.isBargain(), "800,100 vs 1M (19.99% below) must NOT be a bargain");
    assertEquals(20.0, item1999.getPercentageBelowMedian()); // rounded to 20.0 for display, but raw pctBelow is 19.99

    assertEquals(1, stats.getBargainCount());
    MarketSegmentStats segment = stats.getSegments().getFirst();
    assertEquals(1, segment.getBargainCount());
  }

  @Test
  @DisplayName("Empirical Generative Fuzzing: Invariant check across 200 random listings")
  void testGenerativeFuzzingInvariants() {
    Random rng = new Random(42);
    String[] cities = {"Budapest", "Debrecen", "Szeged", "Miskolc", "Pecs", "Gyor"};
    PropertyType[] types = {PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK};

    List<IngatlanItem> randomItems = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      String city = cities[rng.nextInt(cities.length)];
      PropertyType type = types[rng.nextInt(types.length)];
      long priceHuf = (long) (10_000_000L + rng.nextDouble() * 90_000_000L);
      double areaM2 = 30.0 + rng.nextDouble() * 100.0;
      long priceM2 = Math.round((double) priceHuf / areaM2);

      IngatlanItem it = createItem("rnd_" + i, city, type, priceHuf, areaM2, priceM2);
      if (rng.nextDouble() < 0.1) {
        it.setQualityScore(40); // 10% low quality listings
      }
      randomItems.add(it);
    }

    MarketStats stats = engine.calculateMarketStats(randomItems);
    assertNotNull(stats);

    // Invariant 1: Sum of segment bargains == total bargains
    int totalBargainsInSegments = stats.getSegments().stream()
        .mapToInt(MarketSegmentStats::getBargainCount)
        .sum();
    assertEquals(stats.getBargainCount(), totalBargainsInSegments,
        "Total bargains must equal the sum of segment bargains");

    // Invariant 2: For every segment, bargainCount equals actual count of items in that segment with isBargain == true
    for (MarketSegmentStats seg : stats.getSegments()) {
      long itemsMarkedBargainInSeg = randomItems.stream()
          .filter(it -> seg.getCity().equalsIgnoreCase(it.getCity()) && seg.getPropertyType() == it.getPropertyType())
          .filter(IngatlanItem::isBargain)
          .count();
      assertEquals(seg.getBargainCount(), (int) itemsMarkedBargainInSeg,
          "Segment bargainCount must match items in that segment with isBargain == true");

      // Invariant 3: If insufficientData, sampleSize < 5 and median is null and bargainCount is 0
      if (seg.isInsufficientData()) {
        assertTrue(seg.getSampleSize() < 5);
        assertNull(seg.getMedianPricePerM2());
        assertEquals(0, seg.getBargainCount());
      } else {
        assertTrue(seg.getSampleSize() >= 5);
        assertNotNull(seg.getMedianPricePerM2());
      }
    }
  }
}
