package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.presentation.MarketStatsDto;

/**
 * Adversarial and empirical stress test suite for DataQualityEngine and MarketAnalyticsEngine (#267).
 * Validates extreme boundaries, outlier infiltration resistance, and null safety.
 */
class DataQualityAdversarialTest {

  private DataQualityEngine qualityEngine;
  private MarketAnalyticsEngine analyticsEngine;

  @BeforeEach
  void setUp() {
    qualityEngine = new DataQualityEngine();
    analyticsEngine = new MarketAnalyticsEngine(qualityEngine);
  }

  private IngatlanItem createBaselineResidential(String id, long priceHuf, double areaM2, Double rooms) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setUrl("https://portal.example.com/item/" + id);
    item.setTitle("Normál lakás " + id);
    item.setCity("Debrecen");
    item.setLocation("Debrecen, Belváros");
    item.setPropertyType(PropertyType.LAKAS);
    item.setImageUrl("https://portal.example.com/img/" + id + ".jpg");

    item.setPrice(Math.round(priceHuf / 1_000_000.0) + " M Ft");
    item.setPriceInfo(new PriceInfo(item.getPrice(), priceHuf, priceHuf / 1_000_000.0));

    long pricePerM2 = areaM2 > 0 ? Math.round((double) priceHuf / areaM2) : 0L;
    item.setDetails(areaM2 + " m² • " + (rooms != null ? rooms : 0.0) + " szoba");
    item.setDetailsInfo(new PropertyDetails(item.getDetails(), areaM2, rooms != null ? rooms : 0.0, pricePerM2));

    return item;
  }

  private IngatlanItem createBaselinePlot(String id, long priceHuf, double areaM2) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setUrl("https://portal.example.com/plot/" + id);
    item.setTitle("Közművesített építési telek " + id);
    item.setCity("Debrecen");
    item.setLocation("Debrecen, Kerekestelep");
    item.setPropertyType(PropertyType.TELEK);
    item.setImageUrl("https://portal.example.com/img/plot-" + id + ".jpg");

    item.setPrice(Math.round(priceHuf / 1_000_000.0) + " M Ft");
    item.setPriceInfo(new PriceInfo(item.getPrice(), priceHuf, priceHuf / 1_000_000.0));

    long pricePerM2 = areaM2 > 0 ? Math.round((double) priceHuf / areaM2) : 0L;
    item.setDetails(areaM2 + " m²");
    item.setDetailsInfo(new PropertyDetails(item.getDetails(), areaM2, 0.0, pricePerM2));

    return item;
  }

  // =========================================================================
  // 1. Extreme Out-of-Bounds Prices
  // =========================================================================

  @Test
  @DisplayName("Adversarial 1.1: 0 Ft price (numeric & string) receives price sanity deduction")
  void testZeroPriceDeduction() {
    IngatlanItem itemNumeric = createBaselineResidential("p-zero-num", 0L, 50.0, 2.0);
    DataQualityEngine.QualityEvaluation evalNum = qualityEngine.evaluate(itemNumeric);
    assertTrue(evalNum.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));

    IngatlanItem itemString = createBaselineResidential("p-zero-str", 50_000_000L, 50.0, 2.0);
    itemString.setPriceInfo(null);
    itemString.setPrice("0 Ft");
    DataQualityEngine.QualityEvaluation evalStr = qualityEngine.evaluate(itemString);
    assertTrue(evalStr.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));
  }

  @Test
  @DisplayName("Adversarial 1.2: Negative prices receive price sanity deduction")
  void testNegativePriceDeduction() {
    IngatlanItem itemNum = createBaselineResidential("p-neg", -50_000_000L, 50.0, 2.0);
    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(itemNum);
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));
  }

  @Test
  @DisplayName("Adversarial 1.3: Boundary test for prices (999,999 Ft vs 1,000,000 Ft)")
  void testPriceLowerBoundary() {
    IngatlanItem belowMin = createBaselineResidential("p-999k", 999_999L, 50.0, 2.0);
    DataQualityEngine.QualityEvaluation evalBelow = qualityEngine.evaluate(belowMin);
    assertTrue(evalBelow.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));

    // Exactly 1M HUF on a 50m² apartment -> 20,000 Ft/m² (triggers price/m² deduction -20, but NOT price sanity)
    IngatlanItem atMin = createBaselineResidential("p-1m", 1_000_000L, 50.0, 2.0);
    DataQualityEngine.QualityEvaluation evalAt = qualityEngine.evaluate(atMin);
    assertFalse(evalAt.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")),
        "1,000,000 Ft must NOT receive price sanity deduction");
  }

  @Test
  @DisplayName("Adversarial 1.4: 10 Billion Ft and Trillion Ft strings trigger price sanity deduction")
  void testExtremeHighPriceDeduction() {
    IngatlanItem tenBillion = createBaselineResidential("p-10b", 10_000_000_000L, 50.0, 2.0);
    DataQualityEngine.QualityEvaluation eval10b = qualityEngine.evaluate(tenBillion);
    assertTrue(eval10b.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));

    // Exactly 5B HUF -> must NOT trigger price sanity deduction
    IngatlanItem atMax = createBaselineResidential("p-5b", 5_000_000_000L, 1500.0, 10.0);
    DataQualityEngine.QualityEvaluation evalAt = qualityEngine.evaluate(atMax);
    assertFalse(evalAt.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")),
        "5,000,000,000 Ft must NOT receive price sanity deduction");

    // Trillion string via raw price string
    IngatlanItem trillion = createBaselineResidential("p-trillion", 50_000_000L, 50.0, 2.0);
    trillion.setPriceInfo(null);
    trillion.setPrice("1 000 000 000 000 Ft");
    DataQualityEngine.QualityEvaluation evalTrillion = qualityEngine.evaluate(trillion);
    assertTrue(evalTrillion.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));

    // Long overflow string (100 digits)
    IngatlanItem hugeOverflow = createBaselineResidential("p-overflow", 50_000_000L, 50.0, 2.0);
    hugeOverflow.setPriceInfo(null);
    hugeOverflow.setPrice("9999999999999999999999999999999999999999999999999999999999999 Ft");
    assertDoesNotThrow(() -> {
      DataQualityEngine.QualityEvaluation evalOverflow = qualityEngine.evaluate(hugeOverflow);
      assertTrue(evalOverflow.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));
    });
  }

  // =========================================================================
  // 2. Residential vs Plot Boundary Tests
  // =========================================================================

  @Test
  @DisplayName("Adversarial 2.1: Legitimate plot with 10,000 Ft/m² and 0 rooms must NEVER drop below 70")
  void testLegitimatePlotNeverDropsBelow70() {
    // 10M Ft, 1000 m² = 10,000 Ft/m² (< 100k residential floor), 0 rooms
    IngatlanItem plot = createBaselinePlot("plot-10k", 10_000_000L, 1000.0);
    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(plot);

    assertEquals(100, eval.score(), "Complete legitimate plot should have 100 score");
    assertTrue(eval.deductions().isEmpty(), "Plot should have zero deductions");
    assertTrue(eval.score() >= DataQualityEngine.PASSING_THRESHOLD);

    // Plot missing imageUrl (-10) and title (-10)
    plot.setImageUrl(null);
    plot.setTitle(" ");
    DataQualityEngine.QualityEvaluation evalDegraded = qualityEngine.evaluate(plot);
    assertEquals(80, evalDegraded.score());
    assertTrue(evalDegraded.score() >= DataQualityEngine.PASSING_THRESHOLD);
  }

  @Test
  @DisplayName("Adversarial 2.2: Tiny apartment with 5 m² must receive area deduction")
  void testTinyApartmentAreaDeduction() {
    IngatlanItem tiny = createBaselineResidential("apt-5m2", 20_000_000L, 5.0, 1.0);
    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(tiny);

    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));
    assertTrue(eval.score() < DataQualityEngine.PASSING_THRESHOLD,
        "5 m² apartment must drop below 70 passing threshold");
  }

  @Test
  @DisplayName("Adversarial 2.3: Residential area boundaries (9.9 m² vs 10.0 m² and 2000.0 m² vs 2000.1 m²)")
  void testResidentialAreaBoundaries() {
    IngatlanItem below = createBaselineResidential("apt-9.9", 30_000_000L, 9.9, 1.0);
    assertTrue(qualityEngine.evaluate(below).deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));

    IngatlanItem atMin = createBaselineResidential("apt-10", 30_000_000L, 10.0, 0.5);
    assertFalse(qualityEngine.evaluate(atMin).deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));

    IngatlanItem atMax = createBaselineResidential("apt-2000", 800_000_000L, 2000.0, 20.0);
    assertFalse(qualityEngine.evaluate(atMax).deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));

    IngatlanItem above = createBaselineResidential("apt-2000.1", 800_000_000L, 2000.1, 20.0);
    assertTrue(qualityEngine.evaluate(above).deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));
  }

  @Test
  @DisplayName("Adversarial 2.4: Plot area boundaries (49.9 m² vs 50.0 m² and 500,000 m² vs 500,001 m²)")
  void testPlotAreaBoundaries() {
    IngatlanItem below = createBaselinePlot("plot-49.9", 5_000_000L, 49.9);
    assertTrue(qualityEngine.evaluate(below).deductions().stream().anyMatch(d -> d.contains("Irreális telekméret")));

    IngatlanItem atMin = createBaselinePlot("plot-50", 5_000_000L, 50.0);
    assertFalse(qualityEngine.evaluate(atMin).deductions().stream().anyMatch(d -> d.contains("Irreális telekméret")));

    IngatlanItem atMax = createBaselinePlot("plot-500k", 200_000_000L, 500_000.0);
    assertFalse(qualityEngine.evaluate(atMax).deductions().stream().anyMatch(d -> d.contains("Irreális telekméret")));

    IngatlanItem above = createBaselinePlot("plot-500k1", 200_000_000L, 500_001.0);
    assertTrue(qualityEngine.evaluate(above).deductions().stream().anyMatch(d -> d.contains("Irreális telekméret")));
  }

  // =========================================================================
  // 3. Micro-Apartment Room Density
  // =========================================================================

  @Test
  @DisplayName("Adversarial 3.1: Micro-apartment 10 m² with 3 rooms (<12 m²/szoba) receives -15 deduction")
  void testMicroApartmentDensity() {
    // 10 m² with 3 rooms = 3.33 m²/room (< 12.0)
    IngatlanItem item = createBaselineResidential("micro-3room", 25_000_000L, 10.0, 3.0);
    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(item);

    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));
    assertEquals(85, eval.score()); // -15
  }

  @Test
  @DisplayName("Adversarial 3.2: Sparse listing 300 m² with 1 room (>150 m²/szoba) receives -15 deduction")
  void testSparseApartmentDensity() {
    // 300 m² with 1 room = 300 m²/room (> 150.0)
    IngatlanItem item = createBaselineResidential("sparse-1room", 180_000_000L, 300.0, 1.0);
    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(item);

    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));
    assertEquals(85, eval.score()); // -15
  }

  @Test
  @DisplayName("Adversarial 3.3: Room density exact boundaries (12.0 m²/room and 150.0 m²/room)")
  void testRoomDensityExactBoundaries() {
    // 24 m² with 2 rooms = 12.0 m²/room -> exact lower bound
    IngatlanItem atLower = createBaselineResidential("exact-lower", 24_000_000L, 24.0, 2.0);
    assertFalse(qualityEngine.evaluate(atLower).deductions().stream()
        .anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));

    // 23.9 m² with 2 rooms = 11.95 m²/room -> below lower bound
    IngatlanItem belowLower = createBaselineResidential("below-lower", 24_000_000L, 23.9, 2.0);
    assertTrue(qualityEngine.evaluate(belowLower).deductions().stream()
        .anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));

    // 150 m² with 1 room = 150.0 m²/room -> exact upper bound
    IngatlanItem atUpper = createBaselineResidential("exact-upper", 120_000_000L, 150.0, 1.0);
    assertFalse(qualityEngine.evaluate(atUpper).deductions().stream()
        .anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));

    // 150.1 m² with 1 room = 150.1 m²/room -> above upper bound
    IngatlanItem aboveUpper = createBaselineResidential("above-upper", 120_000_000L, 150.1, 1.0);
    assertTrue(qualityEngine.evaluate(aboveUpper).deductions().stream()
        .anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));
  }

  // =========================================================================
  // 4. Outlier Infiltration Attempt
  // =========================================================================

  @Test
  @DisplayName("Adversarial 4.1: Corrupt listing (< 70) cannot warp residential median")
  void testCorruptListingCannotWarpMedian() {
    List<IngatlanItem> items = new ArrayList<>();
    // 5 clean listings: 600k, 700k, 800k, 900k, 1000k -> median = 800k
    items.add(createBaselineResidential("c1", 30_000_000L, 50.0, 2.0)); // 600k
    items.add(createBaselineResidential("c2", 35_000_000L, 50.0, 2.0)); // 700k
    items.add(createBaselineResidential("c3", 40_000_000L, 50.0, 2.0)); // 800k
    items.add(createBaselineResidential("c4", 45_000_000L, 50.0, 2.0)); // 900k
    items.add(createBaselineResidential("c5", 50_000_000L, 50.0, 2.0)); // 1000k

    // Corrupt infiltration candidate with 10,000,000 Ft/m² and quality score 40
    IngatlanItem corruptHigh = createBaselineResidential("infiltrator-high", 500_000_000L, 50.0, 2.0);
    corruptHigh.setQualityScore(40);
    items.add(corruptHigh);

    // Corrupt infiltration candidate with 5,000 Ft/m² and quality score 69 (just below 70)
    IngatlanItem corruptLow = createBaselineResidential("infiltrator-low", 250_000L, 50.0, 2.0);
    corruptLow.setQualityScore(69);
    items.add(corruptLow);

    MarketStatsDto stats = analyticsEngine.calculateMarketStats(items);

    assertNotNull(stats);
    assertEquals(5, stats.getResidentialSampleSize(), "Only the 5 clean listings should be sampled");
    assertEquals(800_000L, stats.getResidentialMedianPricePerM2(), "Median must remain completely unskewed at 800k");
  }

  @Test
  @DisplayName("Adversarial 4.2: Corrupt listing (< 70) cannot receive bargain status under any edge condition")
  void testCorruptListingCannotReceiveBargain() {
    List<IngatlanItem> items = new ArrayList<>();
    // 5 clean listings at 1,000,000 Ft/m² -> median = 1,000,000 Ft/m²
    for (int i = 1; i <= 5; i++) {
      items.add(createBaselineResidential("norm-" + i, 50_000_000L, 50.0, 2.0));
    }

    // Corrupt candidate with 200,000 Ft/m² (-80% below median), score 69
    IngatlanItem corruptCheap = createBaselineResidential("cheap-corrupt", 10_000_000L, 50.0, 2.0);
    corruptCheap.getDetailsInfo().setPricePerSquareMeter(200_000L);
    corruptCheap.setQualityScore(69);
    items.add(corruptCheap);

    // Dynamic candidate with score evaluated < 70 on the fly (null qualityScore)
    // 250k HUF price (< 1M) and 5,000 Ft/m² (< 100k) -> score = 100 - 30 - 20 = 50 (< 70)
    IngatlanItem dynamicCorrupt = createBaselineResidential("dyn-corrupt", 250_000L, 50.0, 2.0);
    dynamicCorrupt.setQualityScore(null);
    items.add(dynamicCorrupt);

    analyticsEngine.calculateMarketStats(items);

    assertFalse(corruptCheap.getIsBargain(), "Listing with score 69 must NEVER qualify as bargain");
    assertNull(corruptCheap.getReferenceMedianPricePerSquareMeter());
    assertNull(corruptCheap.getPercentageBelowMedian());

    assertFalse(dynamicCorrupt.getIsBargain(), "Dynamically evaluated score 50 must NEVER qualify as bargain");
  }

  @Test
  @DisplayName("Adversarial 4.3: Score exactly 70 passes isEligibleForMedian whereas 69 fails")
  void testScoreThresholdBoundary69vs70() {
    IngatlanItem item69 = createBaselineResidential("item-69", 40_000_000L, 50.0, 2.0);
    item69.setQualityScore(69);
    assertFalse(analyticsEngine.isEligibleForMedian(item69), "Score 69 must fail median eligibility");

    IngatlanItem item70 = createBaselineResidential("item-70", 40_000_000L, 50.0, 2.0);
    item70.setQualityScore(70);
    assertTrue(analyticsEngine.isEligibleForMedian(item70), "Score 70 must pass median eligibility");
  }

  @Test
  @DisplayName("Adversarial 4.4: Empirical stress on plot price sanity deduction (-30)")
  void testPlotPriceSanityThresholdEmpiricalAnalysis() {
    // 5 clean plots at 10,000 Ft/m² (10M Ft for 1000 m²) -> median = 10,000 Ft/m²
    List<IngatlanItem> items = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      items.add(createBaselinePlot("clean-plot-" + i, 10_000_000L, 1000.0));
    }

    // Corrupt plot with price 500,000 Ft (< 1M Ft, violates price sanity)
    IngatlanItem corruptPlot = createBaselinePlot("cheap-plot", 500_000L, 1000.0);
    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(corruptPlot);

    // Because only price sanity triggers (-30) and plots are exempt from rules 5 & 6,
    // the evaluation score is exactly 70 (100 - 30)
    assertEquals(70, eval.score());
    assertEquals(1, eval.deductions().size());
    assertTrue(eval.deductions().getFirst().contains("1M - 5B Ft"));

    items.add(corruptPlot);
    analyticsEngine.calculateMarketStats(items);

    // Since score is 70, it is eligible for median (score >= 70) and qualifies for bargain
    assertTrue(analyticsEngine.isEligibleForMedian(corruptPlot));
    assertTrue(corruptPlot.getIsBargain(),
        "Plot with score 70 (at the exact passing threshold) qualifies as bargain");
  }

  // =========================================================================
  // 5. Null-Safety Torture Test
  // =========================================================================

  @Test
  @DisplayName("Adversarial 5.1: Null and empty collections to all engine entry points")
  void testNullAndEmptyCollectionsSafety() {
    assertDoesNotThrow(() -> qualityEngine.evaluate(null));
    assertDoesNotThrow(() -> qualityEngine.applyQualityScore(null));
    assertDoesNotThrow(() -> qualityEngine.evaluateAll(null));
    assertDoesNotThrow(() -> qualityEngine.evaluateAll(Collections.emptyList()));
    assertDoesNotThrow(() -> qualityEngine.evaluateAll(Collections.singletonList(null)));

    assertDoesNotThrow(() -> analyticsEngine.calculateMarketStats(null));
    assertDoesNotThrow(() -> analyticsEngine.calculateMarketStats(Collections.emptyList()));
    assertDoesNotThrow(() -> analyticsEngine.calculateMarketStats(Collections.singletonList(null)));
    assertDoesNotThrow(() -> analyticsEngine.isEligibleForMedian(null));
    assertDoesNotThrow(() -> analyticsEngine.resolvePricePerM2(null));
    assertDoesNotThrow(() -> analyticsEngine.calculateMedian(null));
    assertDoesNotThrow(() -> analyticsEngine.calculateMedian(Collections.emptyList()));
    assertDoesNotThrow(() -> analyticsEngine.calculateDoubleMedian(null));
    assertDoesNotThrow(() -> analyticsEngine.calculateDoubleMedian(Collections.emptyList()));
  }

  @Test
  @DisplayName("Adversarial 5.2: IngatlanItem with completely null fields")
  void testCompletelyNullItem() {
    IngatlanItem ghost = new IngatlanItem();
    assertDoesNotThrow(() -> {
      DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(ghost);
      assertNotNull(eval);
      assertEquals(0, eval.score());
      assertFalse(eval.deductions().isEmpty());
    });

    assertDoesNotThrow(() -> {
      assertFalse(analyticsEngine.isEligibleForMedian(ghost));
      assertNull(analyticsEngine.resolvePricePerM2(ghost));
    });
  }

  @Test
  @DisplayName("Adversarial 5.3: IngatlanItem with blank and whitespace-only fields")
  void testWhitespaceOnlyFields() {
    IngatlanItem blank = new IngatlanItem();
    blank.setId("   ");
    blank.setUrl("\t\n");
    blank.setTitle("   ");
    blank.setCity("   ");
    blank.setLocation("   ");
    blank.setPrice("   ");
    blank.setDetails("   ");
    blank.setImageUrl("   ");

    DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(blank);
    assertEquals(0, eval.score());
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen település")));
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen hirdetés URL")));
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó hirdetéscím")));
  }

  @Test
  @DisplayName("Adversarial 5.4: Unparseable alphanumeric garbage in price and details")
  void testGarbageStringsInPriceAndDetails() {
    IngatlanItem garbage = new IngatlanItem();
    garbage.setUrl("https://portal.hu/1");
    garbage.setTitle("Cím");
    garbage.setCity("Debrecen");
    garbage.setPropertyType(PropertyType.HAZ);
    garbage.setPrice("Hívjon a részletekért!");
    garbage.setDetails("Kérdezzen bátran szobákról!");

    assertDoesNotThrow(() -> {
      DataQualityEngine.QualityEvaluation eval = qualityEngine.evaluate(garbage);
      assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));
      assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen alapterület")));
      assertTrue(eval.score() < DataQualityEngine.PASSING_THRESHOLD);
    });
  }
}
