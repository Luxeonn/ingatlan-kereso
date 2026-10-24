package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.presentation.MarketSegmentStatsDto;
import ingatlanok.presentation.MarketStatsDto;

class MarketAnalyticsEngineTest {

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
      Long pricePerM2,
      Long priceDelta,
      Double priceChangePercent) {
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
    item.setPriceDelta(priceDelta);
    item.setPriceChangePercent(priceChangePercent);
    return item;
  }

  @Test
  @DisplayName("Odd sample size median: 5 items -> exact middle element")
  void testOddSampleSizeMedian() {
    // 5 residential apartments in Debrecen: 600k, 700k, 800k, 900k, 1_000k
    List<IngatlanItem> items = List.of(
        createItem("1", "Debrecen", PropertyType.LAKAS, 30_000_000L, 50.0, 600_000L, null, null),
        createItem("2", "Debrecen", PropertyType.LAKAS, 35_000_000L, 50.0, 700_000L, null, null),
        createItem("3", "Debrecen", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L, null, null),
        createItem("4", "Debrecen", PropertyType.LAKAS, 45_000_000L, 50.0, 900_000L, null, null),
        createItem("5", "Debrecen", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null));

    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    assertFalse(stats.isInsufficientData());
    assertFalse(stats.isResidentialInsufficientData());
    assertEquals(800_000L, stats.getResidentialMedianPricePerM2());
    assertEquals(800_000L, stats.getMedianPricePerM2());
    assertEquals(5, stats.getResidentialSampleSize());
    assertEquals(5, stats.getTotalSampleSize());
  }

  @Test
  @DisplayName("Even sample size median: 6 items -> average of 2 middle elements")
  void testEvenSampleSizeMedian() {
    // 6 residential apartments in Szeged: 600k, 700k, 800k, 900k, 1000k, 1200k
    // Middle two: 800k and 900k -> average = 850k
    List<IngatlanItem> items = List.of(
        createItem("1", "Szeged", PropertyType.LAKAS, 30_000_000L, 50.0, 600_000L, null, null),
        createItem("2", "Szeged", PropertyType.LAKAS, 35_000_000L, 50.0, 700_000L, null, null),
        createItem("3", "Szeged", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L, null, null),
        createItem("4", "Szeged", PropertyType.LAKAS, 45_000_000L, 50.0, 900_000L, null, null),
        createItem("5", "Szeged", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null),
        createItem("6", "Szeged", PropertyType.LAKAS, 60_000_000L, 50.0, 1_200_000L, null, null));

    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    assertFalse(stats.isInsufficientData());
    assertEquals(850_000L, stats.getResidentialMedianPricePerM2());
    assertEquals(850_000L, stats.getMedianPricePerM2());
    assertEquals(6, stats.getResidentialSampleSize());
  }

  @Test
  @DisplayName("Sample size guard < 5: insufficientData = true, median = null, bargains suppressed")
  void testInsufficientDataGuard() {
    // 4 items -> insufficient data
    List<IngatlanItem> items = List.of(
        createItem("1", "Miskolc", PropertyType.HAZ, 20_000_000L, 100.0, 200_000L, null, null),
        createItem("2", "Miskolc", PropertyType.HAZ, 25_000_000L, 100.0, 250_000L, null, null),
        createItem("3", "Miskolc", PropertyType.HAZ, 30_000_000L, 100.0, 300_000L, null, null),
        createItem("4", "Miskolc", PropertyType.HAZ, 35_000_000L, 100.0, 350_000L, null, null));

    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    assertTrue(stats.isInsufficientData());
    assertTrue(stats.isResidentialInsufficientData());
    assertNull(stats.getResidentialMedianPricePerM2());
    assertNull(stats.getMedianPricePerM2());
    assertEquals(4, stats.getResidentialSampleSize());
    assertEquals(4, stats.getTotalSampleSize());
    assertEquals(0, stats.getBargainCount());

    for (IngatlanItem item : items) {
      assertFalse(item.isBargain());
      assertNull(item.getReferenceMedianPricePerSquareMeter());
    }
  }

  @Test
  @DisplayName("Strict residential living vs plot area separation")
  void testStrictResidentialVsPlotSeparation() {
    // 5 residential houses (living area Ft/m2 ~ 500k-700k)
    // 5 plots (land area Ft/m2 ~ 20k-40k)
    List<IngatlanItem> items = new ArrayList<>();
    items.add(createItem("r1", "Gyor", PropertyType.HAZ, 50_000_000L, 100.0, 500_000L, null, null));
    items.add(createItem("r2", "Gyor", PropertyType.HAZ, 55_000_000L, 100.0, 550_000L, null, null));
    items.add(createItem("r3", "Gyor", PropertyType.HAZ, 60_000_000L, 100.0, 600_000L, null, null));
    items.add(createItem("r4", "Gyor", PropertyType.HAZ, 65_000_000L, 100.0, 650_000L, null, null));
    items.add(createItem("r5", "Gyor", PropertyType.HAZ, 70_000_000L, 100.0, 700_000L, null, null));

    items.add(createItem("p1", "Gyor", PropertyType.TELEK, 20_000_000L, 1000.0, 20_000L, null, null));
    items.add(createItem("p2", "Gyor", PropertyType.TELEK, 25_000_000L, 1000.0, 25_000L, null, null));
    items.add(createItem("p3", "Gyor", PropertyType.TELEK, 30_000_000L, 1000.0, 30_000L, null, null));
    items.add(createItem("p4", "Gyor", PropertyType.TELEK, 35_000_000L, 1000.0, 35_000L, null, null));
    items.add(createItem("p5", "Gyor", PropertyType.TELEK, 40_000_000L, 1000.0, 40_000L, null, null));

    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    // Residential median must be 600,000 Ft/m2
    assertEquals(600_000L, stats.getResidentialMedianPricePerM2());
    assertEquals(5, stats.getResidentialSampleSize());
    assertFalse(stats.isResidentialInsufficientData());

    // Plot median must be 30,000 Ft/m2
    assertEquals(30_000L, stats.getPlotMedianPricePerM2());
    assertEquals(5, stats.getPlotSampleSize());
    assertFalse(stats.isPlotInsufficientData());

    // Total sample size is 10
    assertEquals(10, stats.getTotalSampleSize());
    // Since residential is present, top-level medianPricePerM2 reflects residential
    assertEquals(600_000L, stats.getMedianPricePerM2());
  }

  @Test
  @DisplayName("Bargain detection threshold: >=20% below median -> true, 19% below -> false")
  void testBargainDetectionThreshold() {
    // 5 base items with median 1_000_000 Ft/m2 in Budapest
    // Item A at 750k (25% below median) -> bargain!
    // Item B at 810k (19% below median) -> not bargain!
    IngatlanItem item1 = createItem("1", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null);
    IngatlanItem item2 = createItem("2", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null);
    IngatlanItem item3 = createItem("3", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null);
    IngatlanItem item4 = createItem("4", "Budapest", PropertyType.LAKAS, 55_000_000L, 50.0, 1_100_000L, null, null);
    IngatlanItem item5 = createItem("5", "Budapest", PropertyType.LAKAS, 60_000_000L, 50.0, 1_200_000L, null, null);

    // Median of [750k, 810k, 1M, 1M, 1M, 1.1M, 1.2M]
    // 7 elements -> 4th element (index 3) is 1_000_000L!
    IngatlanItem bargainItem =
        createItem("b1", "Budapest", PropertyType.LAKAS, 37_500_000L, 50.0, 750_000L, null, null);
    IngatlanItem regularItem =
        createItem("r1", "Budapest", PropertyType.LAKAS, 40_500_000L, 50.0, 810_000L, null, null);

    List<IngatlanItem> items = List.of(bargainItem, regularItem, item1, item2, item3, item4, item5);
    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    assertEquals(1_000_000L, stats.getResidentialMedianPricePerM2());

    // bargainItem: 750k vs 1M median -> 25.0% below -> isBargain = true
    assertTrue(bargainItem.isBargain());
    assertEquals(1_000_000L, bargainItem.getReferenceMedianPricePerSquareMeter());
    assertEquals(25.0, bargainItem.getPercentageBelowMedian());
    assertEquals(7, bargainItem.getReferenceSampleSize());

    // regularItem: 810k vs 1M median -> 19.0% below -> isBargain = false
    assertFalse(regularItem.isBargain());
    assertEquals(1_000_000L, regularItem.getReferenceMedianPricePerSquareMeter());
    assertEquals(19.0, regularItem.getPercentageBelowMedian());
    assertEquals(7, regularItem.getReferenceSampleSize());

    assertEquals(1, stats.getBargainCount());
    assertEquals(14.3, stats.getBargainRatePercent());
  }

  @Test
  @DisplayName("Price drops calculation and top drops ordering")
  void testPriceDropMetricsCalculation() {
    IngatlanItem drop1 = createItem("d1", "Pecs", PropertyType.LAKAS, 36_000_000L, 60.0, 600_000L, -4_000_000L, -10.0);
    IngatlanItem drop2 = createItem("d2", "Pecs", PropertyType.LAKAS, 48_000_000L, 60.0, 800_000L, -2_000_000L, -4.0);
    IngatlanItem noDrop = createItem("n1", "Pecs", PropertyType.LAKAS, 50_000_000L, 60.0, 833_333L, null, null);
    IngatlanItem priceIncrease =
        createItem("u1", "Pecs", PropertyType.LAKAS, 55_000_000L, 60.0, 916_667L, 5_000_000L, 10.0);

    List<IngatlanItem> items = List.of(drop1, drop2, noDrop, priceIncrease);
    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    assertEquals(2, stats.getPriceDropCount());
    assertEquals(50.0, stats.getPriceDropRatePercent()); // 2 out of 4 items
    assertEquals(3_000_000L, stats.getAveragePriceDropAmountHuf()); // (4M + 2M) / 2 = 3M
    assertEquals(7.0, stats.getAveragePriceDropPercent()); // (10.0 + 4.0) / 2 = 7.0

    assertNotNull(stats.getTopPriceDrops());
    assertEquals(2, stats.getTopPriceDrops().size());
    // Largest drop first: drop1 (-4M) then drop2 (-2M)
    assertEquals("d1", stats.getTopPriceDrops().get(0).getId());
    assertEquals(-4_000_000L, stats.getTopPriceDrops().get(0).getPriceDelta());
    assertEquals("d2", stats.getTopPriceDrops().get(1).getId());
    assertEquals(-2_000_000L, stats.getTopPriceDrops().get(1).getPriceDelta());
  }

  @Test
  @DisplayName("Zero and null robustness: empty/null list, missing details, zero area")
  void testRobustnessToZeroAndNullValues() {
    assertNotNull(engine.calculateMarketStats(null));
    assertNotNull(engine.calculateMarketStats(List.of()));

    IngatlanItem emptyItem = new IngatlanItem();
    emptyItem.setId("empty");
    emptyItem.setUrl("http://empty");

    IngatlanItem zeroArea = createItem("z1", "Eger", PropertyType.LAKAS, 20_000_000L, 0.0, null, null, null);
    IngatlanItem negPrice = createItem("z2", "Eger", PropertyType.LAKAS, -10_000_000L, 50.0, null, null, null);

    List<IngatlanItem> items = new ArrayList<>();
    items.add(null);
    items.add(emptyItem);
    items.add(zeroArea);
    items.add(negPrice);

    MarketStatsDto stats = engine.calculateMarketStats(items);
    assertNotNull(stats);
    assertTrue(stats.isInsufficientData());
    assertNull(stats.getMedianPricePerM2());
    assertEquals(0, stats.getTotalSampleSize());
    assertNull(engine.resolvePricePerM2(null));
    assertNull(engine.resolvePricePerM2(emptyItem));
    assertNull(engine.resolvePricePerM2(zeroArea));
    assertNull(engine.calculateMedian(null));
    assertNull(engine.calculateMedian(List.of()));
    assertNull(engine.calculateDoubleMedian(null));
    assertNull(engine.calculateDoubleMedian(List.of()));
  }

  @Test
  @DisplayName("Deduplication prevents repeated listings from skewing medians")
  void testDeduplicationBeforeMedian() {
    // 5 unique listings: 500k, 600k, 700k, 800k, 900k -> median = 700k
    // duplicate listing of 500k 10 times with same URL
    IngatlanItem item1 = createItem("1", "Debrecen", PropertyType.LAKAS, 25_000_000L, 50.0, 500_000L, null, null);
    IngatlanItem item2 = createItem("2", "Debrecen", PropertyType.LAKAS, 30_000_000L, 50.0, 600_000L, null, null);
    IngatlanItem item3 = createItem("3", "Debrecen", PropertyType.LAKAS, 35_000_000L, 50.0, 700_000L, null, null);
    IngatlanItem item4 = createItem("4", "Debrecen", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L, null, null);
    IngatlanItem item5 = createItem("5", "Debrecen", PropertyType.LAKAS, 45_000_000L, 50.0, 900_000L, null, null);

    List<IngatlanItem> itemsWithDuplicates = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      itemsWithDuplicates.add(item1);
    }
    itemsWithDuplicates.addAll(List.of(item2, item3, item4, item5));

    MarketStatsDto stats = engine.calculateMarketStats(itemsWithDuplicates);
    assertNotNull(stats);
    // Deduplication should ensure sample size is 5 and median is 700k, not 500k!
    assertEquals(5, stats.getTotalSampleSize());
    assertEquals(700_000L, stats.getResidentialMedianPricePerM2());
  }

  @Test
  @DisplayName("Plot-only search provides plot median as top-level median")
  void testPlotOnlySearchFallback() {
    List<IngatlanItem> plots = List.of(
        createItem("p1", "Kecskemet", PropertyType.TELEK, 10_000_000L, 1000.0, 10_000L, null, null),
        createItem("p2", "Kecskemet", PropertyType.TELEK, 15_000_000L, 1000.0, 15_000L, null, null),
        createItem("p3", "Kecskemet", PropertyType.TELEK, 20_000_000L, 1000.0, 20_000L, null, null),
        createItem("p4", "Kecskemet", PropertyType.TELEK, 25_000_000L, 1000.0, 25_000L, null, null),
        createItem("p5", "Kecskemet", PropertyType.TELEK, 30_000_000L, 1000.0, 30_000L, null, null));

    MarketStatsDto stats = engine.calculateMarketStats(plots);
    assertNotNull(stats);
    assertFalse(stats.isInsufficientData());
    assertEquals(20_000L, stats.getPlotMedianPricePerM2());
    assertEquals(20_000L, stats.getMedianPricePerM2());
    assertEquals(0, stats.getResidentialSampleSize());
    assertTrue(stats.isResidentialInsufficientData());
  }

  @Test
  @DisplayName("Segment breakdown captures city and property type KPIs")
  void testSegmentsBreakdown() {
    List<IngatlanItem> items = List.of(
        createItem("1", "Debrecen", PropertyType.LAKAS, 30_000_000L, 50.0, 600_000L, null, null),
        createItem("2", "Debrecen", PropertyType.LAKAS, 35_000_000L, 50.0, 700_000L, null, null),
        createItem("3", "Debrecen", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L, null, null),
        createItem("4", "Debrecen", PropertyType.LAKAS, 45_000_000L, 50.0, 900_000L, null, null),
        createItem("5", "Debrecen", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null));

    MarketStatsDto stats = engine.calculateMarketStats(items);
    assertNotNull(stats.getSegments());
    assertEquals(1, stats.getSegments().size());

    MarketSegmentStatsDto segment = stats.getSegments().getFirst();
    assertEquals("Debrecen", segment.getCity());
    assertEquals(PropertyType.LAKAS, segment.getPropertyType());
    assertTrue(segment.isResidential());
    assertFalse(segment.isPlot());
    assertEquals(5, segment.getSampleSize());
    assertFalse(segment.isInsufficientData());
    assertEquals(800_000L, segment.getMedianPricePerM2());
    assertEquals(800_000L, segment.getAveragePricePerM2());
    assertEquals(40.0, segment.getMedianPriceMillion());
    assertEquals(40.0, segment.getAveragePriceMillion());
    assertEquals(30.0, segment.getMinPriceMillion());
    assertEquals(50.0, segment.getMaxPriceMillion());
  }

  @Test
  @DisplayName("Adversarial: Odd sample sizes exact mathematical median (5, 7, 9 items)")
  void testOddSampleSizesMathematicalMedian() {
    List<Long> values5 = List.of(900L, 100L, 700L, 300L, 500L);
    assertEquals(500L, engine.calculateMedian(values5));

    List<Long> values7 = List.of(700L, 200L, 600L, 100L, 500L, 300L, 400L);
    assertEquals(400L, engine.calculateMedian(values7));

    List<Long> values9 = List.of(90L, 10L, 80L, 20L, 70L, 30L, 60L, 40L, 50L);
    assertEquals(50L, engine.calculateMedian(values9));
  }

  @Test
  @DisplayName("Adversarial: Even sample sizes exact mathematical median with whole-number rounding")
  void testEvenSampleSizesMathematicalMedianWithRounding() {
    // Even sum: (300 + 400) / 2 = 350
    List<Long> values6Even = List.of(600L, 100L, 500L, 200L, 400L, 300L);
    assertEquals(350L, engine.calculateMedian(values6Even));

    // Odd sum: (301 + 400) / 2 = 350.5 -> rounds to 351
    List<Long> values6Odd = List.of(600L, 100L, 500L, 200L, 400L, 301L);
    assertEquals(351L, engine.calculateMedian(values6Odd));

    // 8 items even sum: (40 + 50) / 2 = 45
    List<Long> values8 = List.of(80L, 10L, 70L, 20L, 60L, 30L, 50L, 40L);
    assertEquals(45L, engine.calculateMedian(values8));

    // 10 items odd sum: middle elements 51 and 60 -> (51 + 60) / 2 = 55.5 -> rounds to 56
    List<Long> values10 = List.of(100L, 10L, 90L, 20L, 80L, 30L, 70L, 40L, 60L, 51L);
    assertEquals(56L, engine.calculateMedian(values10));
  }

  @Test
  @DisplayName("Adversarial: Sample size guard < 5 strictly enforces insufficientData and suppresses bargains")
  void testSampleSizeGuardFor1To4Items() {
    for (int count = 1; count <= 4; count++) {
      List<IngatlanItem> items = new ArrayList<>();
      for (int i = 1; i <= count; i++) {
        // High discount item (300k vs 1M potential)
        items.add(createItem("g" + count + "_" + i, "Debrecen", PropertyType.LAKAS,
            15_000_000L, 50.0, 300_000L, null, null));
      }

      MarketStatsDto stats = engine.calculateMarketStats(items);
      assertNotNull(stats, "Stats must not be null for count " + count);
      assertTrue(stats.isInsufficientData(), "Overall insufficientData must be true for count " + count);
      assertTrue(stats.isResidentialInsufficientData(), "Residential insufficientData must be true for count " + count);
      assertNull(stats.getMedianPricePerM2(), "Median price per m2 must be null for count " + count);
      assertNull(stats.getResidentialMedianPricePerM2(), "Residential median must be null for count " + count);
      assertEquals(0, stats.getBargainCount(), "Bargain count must be 0 for count " + count);

      for (IngatlanItem item : items) {
        assertFalse(item.isBargain(), "Item must not be marked as bargain when sample size < 5");
        assertNull(item.getReferenceMedianPricePerSquareMeter(), "Reference median must be null when sample size < 5");
        assertNull(item.getPercentageBelowMedian(), "Percentage below median must be null when sample size < 5");
      }
    }
  }

  @Test
  @DisplayName("Adversarial: TELEK plot prices (20k Ft/m²) never drag down LAKAS/HAZ living area medians")
  void testTelekNeverPollutesResidentialMedian() {
    List<IngatlanItem> items = new ArrayList<>();
    // 5 residential apartments around 1,000,000 Ft/m²
    items.add(createItem("r1", "Budapest", PropertyType.LAKAS, 45_000_000L, 50.0, 900_000L, null, null));
    items.add(createItem("r2", "Budapest", PropertyType.LAKAS, 48_000_000L, 50.0, 960_000L, null, null));
    items.add(createItem("r3", "Budapest", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null));
    items.add(createItem("r4", "Budapest", PropertyType.LAKAS, 52_000_000L, 50.0, 1_040_000L, null, null));
    items.add(createItem("r5", "Budapest", PropertyType.LAKAS, 55_000_000L, 50.0, 1_100_000L, null, null));

    // 15 low-cost plots (20,000 Ft/m²)
    for (int i = 1; i <= 15; i++) {
      items.add(createItem("p" + i, "Budapest", PropertyType.TELEK, 20_000_000L, 1000.0, 20_000L, null, null));
    }

    MarketStatsDto stats = engine.calculateMarketStats(items);
    assertNotNull(stats);
    // Residential median must remain exactly 1,000,000 Ft/m² despite 15 plot listings
    assertEquals(1_000_000L, stats.getResidentialMedianPricePerM2());
    assertEquals(1_000_000L, stats.getMedianPricePerM2());
    assertEquals(5, stats.getResidentialSampleSize());

    // Plot median must be exactly 20,000 Ft/m²
    assertEquals(20_000L, stats.getPlotMedianPricePerM2());
    assertEquals(15, stats.getPlotSampleSize());
    assertEquals(20, stats.getTotalSampleSize());
  }

  @Test
  @DisplayName("Adversarial: Bargain threshold exact boundary test (20.0% is bargain, 19.9% is not)")
  void testBargainThresholdBoundary20PercentVs19Point9Percent() {
    // 5 baseline apartments in Gyor with identical 1,000,000 Ft/m² price
    List<IngatlanItem> items = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      items.add(createItem("base" + i, "Gyor", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null));
    }

    // Candidate A: 800,000 Ft/m² -> exactly 20.0% below 1,000,000 -> bargain!
    IngatlanItem item20 = createItem("c20", "Gyor", PropertyType.LAKAS, 40_000_000L, 50.0, 800_000L, null, null);
    // Candidate B: 801,000 Ft/m² -> exactly 19.9% below 1,000,000 -> not bargain!
    IngatlanItem item199 = createItem("c199", "Gyor", PropertyType.LAKAS, 40_050_000L, 50.0, 801_000L, null, null);
    // Candidate C: 1,000,000 Ft/m² -> 0% below -> not bargain
    IngatlanItem item0 = createItem("c0", "Gyor", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L, null, null);
    // Candidate D: 1,200,000 Ft/m² -> premium (+20%) -> not bargain
    IngatlanItem itemPrem = createItem("cPrem", "Gyor", PropertyType.LAKAS, 60_000_000L, 50.0, 1_200_000L, null, null);

    items.add(item20);
    items.add(item199);
    items.add(item0);
    items.add(itemPrem);

    MarketStatsDto stats = engine.calculateMarketStats(items);
    assertNotNull(stats);
    assertEquals(1_000_000L, stats.getResidentialMedianPricePerM2());

    // Exact boundary assertions
    assertTrue(item20.isBargain(), "800,000 vs 1,000,000 (20.0% below) must be marked as bargain");
    assertEquals(20.0, item20.getPercentageBelowMedian());

    assertFalse(item199.isBargain(), "801,000 vs 1,000,000 (19.9% below) must NOT be marked as bargain");
    assertEquals(19.9, item199.getPercentageBelowMedian());

    assertFalse(item0.isBargain());
    assertEquals(0.0, item0.getPercentageBelowMedian());

    assertFalse(itemPrem.isBargain());
    assertEquals(-20.0, itemPrem.getPercentageBelowMedian());
  }

  @Test
  @DisplayName("Adversarial: Price drops filter out positive and zero deltas, and sort descending")
  void testPriceDropsPositiveAndZeroDeltasIgnored() {
    IngatlanItem dropBig = createItem("dBig", "Szolnok", PropertyType.LAKAS, 45_000_000L, 50.0, 900_000L,
        -5_000_000L, -10.0);
    IngatlanItem dropMid = createItem("dMid", "Szolnok", PropertyType.LAKAS, 48_000_000L, 50.0, 960_000L,
        -2_000_000L, -4.0);
    IngatlanItem dropSmall = createItem("dSmall", "Szolnok", PropertyType.LAKAS, 49_500_000L, 50.0, 990_000L,
        -500_000L, -1.0);
    IngatlanItem priceHike = createItem("hike", "Szolnok", PropertyType.LAKAS, 55_000_000L, 50.0, 1_100_000L,
        3_000_000L, 5.8);
    IngatlanItem zeroDelta = createItem("zero", "Szolnok", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L,
        0L, 0.0);
    IngatlanItem nullDelta = createItem("nullD", "Szolnok", PropertyType.LAKAS, 50_000_000L, 50.0, 1_000_000L,
        null, null);

    List<IngatlanItem> items = List.of(dropMid, priceHike, dropSmall, zeroDelta, dropBig, nullDelta);
    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    // Exactly 3 drops (dropBig, dropMid, dropSmall)
    assertEquals(3, stats.getPriceDropCount());
    assertEquals(50.0, stats.getPriceDropRatePercent()); // 3 out of 6 items

    // Sorted descending by absolute drop magnitude: dropBig (-5M), dropMid (-2M), dropSmall (-500k)
    assertEquals(3, stats.getTopPriceDrops().size());
    assertEquals("dBig", stats.getTopPriceDrops().get(0).getId());
    assertEquals(-5_000_000L, stats.getTopPriceDrops().get(0).getPriceDelta());
    assertEquals("dMid", stats.getTopPriceDrops().get(1).getId());
    assertEquals(-2_000_000L, stats.getTopPriceDrops().get(1).getPriceDelta());
    assertEquals("dSmall", stats.getTopPriceDrops().get(2).getId());
    assertEquals(-500_000L, stats.getTopPriceDrops().get(2).getPriceDelta());

    // Average drop amount: (5M + 2M + 500k) / 3 = 2_500_000
    assertEquals(2_500_000L, stats.getAveragePriceDropAmountHuf());
    // Average drop percent: (10.0 + 4.0 + 1.0) / 3 = 5.0
    assertEquals(5.0, stats.getAveragePriceDropPercent());
  }

  @Test
  @DisplayName("Adversarial: Malformed, null, negative, and extreme data zero-crash resilience")
  void testMalformedAndCorruptedDataResilience() {
    IngatlanItem item1 = new IngatlanItem();
    item1.setId("m1");
    item1.setPropertyType((PropertyType) null); // explicitly null
    item1.setCity("   "); // whitespace city
    item1.setPriceInfo(null);
    item1.setDetailsInfo(null);

    IngatlanItem item2 = createItem("m2", null, PropertyType.EGYEB_ISMERETLEN, -50_000_000L, -100.0, -500_000L,
        null, null);
    IngatlanItem item3 = createItem("m3", "", PropertyType.GARAZS, 0L, 0.0, 0L, null, null);

    // Extreme huge numbers (checking no overflow)
    IngatlanItem itemHuge = createItem("huge", "Debrecen", PropertyType.HAZ,
        2_000_000_000_000L, 1000.0, 2_000_000_000L, -50_000_000_000L, -2.5);

    List<IngatlanItem> corruptedList = new ArrayList<>();
    corruptedList.add(null);
    corruptedList.add(item1);
    corruptedList.add(item2);
    corruptedList.add(item3);
    corruptedList.add(itemHuge);

    MarketStatsDto stats = engine.calculateMarketStats(corruptedList);
    assertNotNull(stats);
    // itemHuge is rejected by DataQualityEngine (score < 70 due to 2T Ft price and 2B Ft/m²), so sample size is 0
    assertEquals(0, stats.getResidentialSampleSize());
    assertTrue(stats.isResidentialInsufficientData());
    assertTrue(stats.isInsufficientData());
    assertNull(stats.getMedianPricePerM2());
    assertEquals(1, stats.getPriceDropCount());
  }

  @Test
  @DisplayName("Milestone R5: Listings with qualityScore < 70 are excluded from median calculations")
  void testLowQualityListingsExcludedFromMedian() {
    IngatlanItem item1 = createItem("q1", "Debrecen", PropertyType.LAKAS, 48_000_000L, 60.0, 800_000L, null, null);
    IngatlanItem item2 = createItem("q2", "Debrecen", PropertyType.LAKAS, 51_000_000L, 60.0, 850_000L, null, null);
    IngatlanItem item3 = createItem("q3", "Debrecen", PropertyType.LAKAS, 54_000_000L, 60.0, 900_000L, null, null);
    IngatlanItem item4 = createItem("q4", "Debrecen", PropertyType.LAKAS, 57_000_000L, 60.0, 950_000L, null, null);
    IngatlanItem item5 = createItem("q5", "Debrecen", PropertyType.LAKAS, 60_000_000L, 60.0, 1_000_000L, null, null);

    // 1 corrupt outlier listing (qualityScore explicitly set to 40)
    IngatlanItem corruptOutlier =
        createItem("outlier", "Debrecen", PropertyType.LAKAS, 600_000L, 60.0, 10_000L, null, null);
    corruptOutlier.setQualityScore(40);

    List<IngatlanItem> items = List.of(item1, item2, item3, item4, item5, corruptOutlier);
    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertNotNull(stats);
    assertEquals(5, stats.getResidentialSampleSize());
    assertEquals(900_000L, stats.getResidentialMedianPricePerM2());
  }

  @Test
  @DisplayName("Milestone R5: Listings with qualityScore < 70 cannot qualify as bargains")
  void testLowQualityListingsDisallowedFromBargain() {
    IngatlanItem item1 = createItem("b1", "Debrecen", PropertyType.LAKAS, 60_000_000L, 60.0, 1_000_000L, null, null);
    IngatlanItem item2 = createItem("b2", "Debrecen", PropertyType.LAKAS, 60_000_000L, 60.0, 1_000_000L, null, null);
    IngatlanItem item3 = createItem("b3", "Debrecen", PropertyType.LAKAS, 60_000_000L, 60.0, 1_000_000L, null, null);
    IngatlanItem item4 = createItem("b4", "Debrecen", PropertyType.LAKAS, 60_000_000L, 60.0, 1_000_000L, null, null);
    IngatlanItem item5 = createItem("b5", "Debrecen", PropertyType.LAKAS, 60_000_000L, 60.0, 1_000_000L, null, null);

    // Candidate 1: Genuine bargain (-25% below median -> 750,000 Ft/m²), qualityScore = 95
    IngatlanItem genuineBargain =
        createItem("b-gen", "Debrecen", PropertyType.LAKAS, 45_000_000L, 60.0, 750_000L, null, null);
    genuineBargain.setQualityScore(95);

    // Candidate 2: Fake/corrupted low price (-60% below median -> 400,000 Ft/m²), but qualityScore = 50 (< 70)
    IngatlanItem fakeBargain =
        createItem("b-fake", "Debrecen", PropertyType.LAKAS, 24_000_000L, 60.0, 400_000L, null, null);
    fakeBargain.setQualityScore(50);

    List<IngatlanItem> items = List.of(item1, item2, item3, item4, item5, genuineBargain, fakeBargain);
    MarketStatsDto stats = engine.calculateMarketStats(items);

    assertTrue(genuineBargain.getIsBargain(), "Genuine high-quality listing should qualify as bargain");
    assertFalse(fakeBargain.getIsBargain(), "Low quality score listing must NEVER qualify as bargain");
    assertEquals(1, stats.getBargainCount());
  }
}

