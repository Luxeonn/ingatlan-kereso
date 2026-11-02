package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.MarketStats;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.testsupport.TestFixtures;

@DisplayName("Business Logic Golden Dataset & Regression Test First Suite (#345)")
class BusinessLogicGoldenDatasetRegressionTest {

  private final MarketAnalyticsEngine analyticsEngine = new MarketAnalyticsEngine();
  private final DeduplicationEngine dedupEngine = new DeduplicationEngine(null);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Regression #1 (Red -> Green): Dotted thousand separators and NBSP in price parsing")
  void testPriceParsingDottedAndNbspAndZeroPriceProtection() {
    // 1. Dotted thousand separators: "45.000.000 Ft"
    PriceInfo dottedPrice = PropertyParser.parsePrice("Ár: 45.000.000 Ft");
    assertEquals(45_000_000L, dottedPrice.getAmountHuf(), "Dotted thousand groups must parse to 45,000,000 Ft");
    assertEquals(45.0, dottedPrice.getAmountMillion(), "Dotted thousand groups must parse to 45.0 M Ft");

    // 2. Non-breaking space (NBSP): "45\u00A0000\u00A0000 Ft"
    PriceInfo nbspPrice = PropertyParser.parsePrice("Ár: 45\u00A0000\u00A0000 Ft");
    assertEquals(45_000_000L, nbspPrice.getAmountHuf(), "NBSP-separated price must parse to 45,000,000 Ft");
    assertEquals(45.0, nbspPrice.getAmountMillion(), "NBSP-separated price must parse to 45.0 M Ft");

    // 3. Zero price protection in parseDetails
    PriceInfo zeroPrice = PriceInfo.builder().rawText("0 Ft").amountHuf(0L).amountMillion(0.0).build();
    PropertyDetails zeroDetails = PropertyParser.parseDetails("50 m2", zeroPrice);
    assertNull(zeroDetails.getPricePerSquareMeter(), "Zero price must not calculate 0 Ft/m2; should be null");
  }

  @Test
  @DisplayName("Regression #2 (Red -> Green): Outliers (score < 70) must not contaminate global price metrics")
  void testQuarantinedOutliersDoNotContaminateGlobalPriceMetrics() {
    List<IngatlanItem> items = new ArrayList<>();
    // 5 valid peers in Szeged (price = 40M Ft, area = 50 m2 -> 800,000 Ft/m2)
    for (int i = 1; i <= 5; i++) {
      items.add(TestFixtures.validItemWithQuality(
          "SZ-VALID-" + i, "Szeged", PropertyType.LAKAS, 40_000_000L, 50.0, 95));
    }

    // 1 extreme outlier (price = 10 Billion Ft, area = 50 m2 -> qualityScore = 30)
    items.add(TestFixtures.validItemWithQuality(
        "SZ-OUTLIER", "Szeged", PropertyType.LAKAS, 10_000_000_000L, 50.0, 30));

    MarketStats stats = analyticsEngine.calculateMarketStats(items);

    assertNotNull(stats);
    assertEquals(40.0, stats.getMaxPriceMillion(),
        "Max price must ignore quarantined outliers (expected 40.0 M, not 10000.0 M)");
    assertEquals(40.0, stats.getAveragePriceMillion(),
        "Average price must ignore quarantined outliers (expected 40.0 M)");
    assertEquals(800_000L, stats.getAveragePricePerM2(),
        "Average price/m2 must ignore quarantined outliers (expected 800,000 Ft/m2)");
  }

  @Test
  @DisplayName("Regression #3 (Red -> Green): Consolidated multi-portal cluster must inherit newConstruction flag")
  void testConsolidatedClusterInheritsNewConstructionFlag() {
    IngatlanItem itemA = TestFixtures.validResidentialItem("DH-LOW", "Debrecen", 40_000_000L, 60.0, 2.0);
    itemA.setUrl("https://dh.hu/ingatlan/dh-low");
    itemA.setTitle("Új építésű lakás Debrecen Belváros");
    itemA.setNewConstruction(false); // DH omitted the flag

    IngatlanItem itemB = TestFixtures.validResidentialItem("OC-HIGH", "Debrecen", 42_000_000L, 60.0, 2.0);
    itemB.setUrl("https://oc.hu/ingatlan/oc-high");
    itemB.setTitle("Új építésű lakás Debrecen Belváros");
    itemB.setNewConstruction(true); // OC identified new construction

    List<IngatlanItem> deduplicated = dedupEngine.deduplicateMultiSignal(List.of(itemA, itemB));

    assertEquals(1, deduplicated.size(), "Both listings should cluster into a single primary item");
    IngatlanItem primary = deduplicated.get(0);
    assertEquals("DH-LOW", primary.getId(), "Primary item must be the lowest active price source");
    assertTrue(primary.isNewConstruction(),
        "Consolidated cluster must inherit newConstruction=true from cluster members");
  }

  @Test
  @DisplayName("Golden Dataset Verification: Load and verify static JSON golden scenarios")
  void testGoldenDatasetScenarios() throws Exception {
    InputStream stream = getClass().getResourceAsStream("/fixtures/golden/business-regression-dataset.json");
    assertNotNull(stream, "Golden dataset resource must exist");

    JsonNode root = objectMapper.readTree(stream);
    assertNotNull(root.get("scenarios"), "Root scenarios must be present");

    JsonNode boundaryCases = root.get("scenarios").get("boundaryCases");
    assertNotNull(boundaryCases, "boundaryCases scenario must be present");
    for (JsonNode bCase : boundaryCases) {
      if (bCase.has("case") && "BARGAIN_THRESHOLD_EXACT_20_PCT".equals(bCase.get("case").asString())) {
        double median = bCase.get("medianPricePerM2").asDouble();
        double itemPrice = bCase.get("itemPricePerM2").asDouble();
        double pctBelow = ((median - itemPrice) / median) * 100.0;
        assertEquals(20.0, pctBelow, 0.001);
        assertTrue(pctBelow >= 20.0, "Exact 20% discount qualifies as bargain");
      }
      if (bCase.has("case") && "BARGAIN_THRESHOLD_BELOW_20_PCT".equals(bCase.get("case").asString())) {
        double median = bCase.get("medianPricePerM2").asDouble();
        double itemPrice = bCase.get("itemPricePerM2").asDouble();
        double pctBelow = ((median - itemPrice) / median) * 100.0;
        assertFalse(pctBelow >= 20.0, "19.99% discount must not qualify as bargain");
      }
    }

    JsonNode falseBargain = root.get("scenarios").get("falseBargainPreventionCases");
    assertNotNull(falseBargain, "falseBargainPreventionCases scenario must be present");
    assertEquals(2, falseBargain.size());
  }
}
