package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;

class DataQualityEngineTest {

  private DataQualityEngine engine;

  @BeforeEach
  void setUp() {
    engine = new DataQualityEngine();
  }

  private IngatlanItem createValidResidentialItem() {
    IngatlanItem item = new IngatlanItem();
    item.setId("valid-1");
    item.setPortal("Duna House");
    item.setUrl("https://dh.hu/ingatlan/123");
    item.setTitle("Kiváló állapotú belvárosi tégla lakás");
    item.setCity("Debrecen");
    item.setLocation("Debrecen, Belváros");
    item.setPropertyType(PropertyType.LAKAS);
    item.setImageUrl("https://img.dh.hu/123.jpg");

    // 45,000,000 HUF, 60 m², 2 rooms -> 750,000 Ft/m² (valid), 30 m²/szoba (valid)
    item.setPrice("45.0 M Ft");
    item.setPriceInfo(new PriceInfo("45.0 M Ft", 45_000_000L, 45.0));

    item.setDetails("60 m² • 2 szoba");
    item.setDetailsInfo(new PropertyDetails("60 m² • 2 szoba", 60.0, 2.0, 750_000L));

    return item;
  }

  @Test
  @DisplayName("evaluate returns score 0 and deduction when item is null")
  void testEvaluateNullItem() {
    DataQualityEngine.QualityEvaluation eval = engine.evaluate(null);
    assertNotNull(eval);
    assertEquals(0, eval.score());
    assertEquals(1, eval.deductions().size());
    assertTrue(eval.deductions().getFirst().contains("Hiányzó hirdetés"));
  }

  @Test
  @DisplayName("evaluate returns perfect 100 score for complete, realistic residential listing")
  void testEvaluatePerfectResidentialListing() {
    IngatlanItem item = createValidResidentialItem();
    DataQualityEngine.QualityEvaluation eval = engine.evaluate(item);

    assertEquals(100, eval.score());
    assertTrue(eval.deductions().isEmpty());
  }

  @Test
  @DisplayName("Price sanity: deductions for null, <1M HUF, and >5B HUF prices")
  void testEvaluatePriceSanity() {
    // Missing price
    IngatlanItem missingPrice = createValidResidentialItem();
    missingPrice.setPriceInfo(null);
    missingPrice.setPrice(null);
    DataQualityEngine.QualityEvaluation evalMissing = engine.evaluate(missingPrice);
    assertTrue(evalMissing.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));
    assertEquals(70, evalMissing.score()); // -30

    // Price < 1M HUF
    IngatlanItem lowPrice = createValidResidentialItem();
    lowPrice.setPriceInfo(new PriceInfo("800 ezer Ft", 800_000L, 0.8));
    DataQualityEngine.QualityEvaluation evalLow = engine.evaluate(lowPrice);
    assertTrue(evalLow.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));

    // Price > 5B HUF
    IngatlanItem highPrice = createValidResidentialItem();
    highPrice.setPriceInfo(new PriceInfo("6 Mrd Ft", 6_000_000_000L, 6000.0));
    DataQualityEngine.QualityEvaluation evalHigh = engine.evaluate(highPrice);
    assertTrue(evalHigh.deductions().stream().anyMatch(d -> d.contains("1M - 5B Ft")));
  }

  @Test
  @DisplayName("Area sanity: residential area bounds (<10 m² or >2000 m²)")
  void testEvaluateAreaSanityResidential() {
    // Area < 10 m²
    IngatlanItem smallArea = createValidResidentialItem();
    smallArea.setDetailsInfo(new PropertyDetails("8 m² • 1 szoba", 8.0, 1.0, 1_000_000L));
    DataQualityEngine.QualityEvaluation evalSmall = engine.evaluate(smallArea);
    assertTrue(evalSmall.deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));
    assertTrue(evalSmall.deductions().stream().anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));

    // Area > 2000 m²
    IngatlanItem hugeArea = createValidResidentialItem();
    hugeArea.setDetailsInfo(new PropertyDetails("2500 m² • 10 szoba", 2500.0, 10.0, 500_000L));
    DataQualityEngine.QualityEvaluation evalHuge = engine.evaluate(hugeArea);
    assertTrue(evalHuge.deductions().stream().anyMatch(d -> d.contains("Irreális lakóterület")));

    // Missing / <=0 area
    IngatlanItem zeroArea = createValidResidentialItem();
    zeroArea.setDetailsInfo(new PropertyDetails("0 m²", 0.0, 0.0, 0L));
    zeroArea.setDetails(null);
    DataQualityEngine.QualityEvaluation evalZero = engine.evaluate(zeroArea);
    assertTrue(evalZero.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen alapterület")));
  }

  @Test
  @DisplayName("Area sanity: plot bounds (<50 m² or >500,000 m²)")
  void testEvaluateAreaSanityPlot() {
    IngatlanItem plot = createValidResidentialItem();
    plot.setPropertyType(PropertyType.TELEK);
    plot.setPriceInfo(new PriceInfo("15 M Ft", 15_000_000L, 15.0));

    // Valid plot area (1200 m²)
    plot.setDetailsInfo(new PropertyDetails("1200 m²", 1200.0, 0.0, 12_500L));
    DataQualityEngine.QualityEvaluation evalValid = engine.evaluate(plot);
    assertEquals(100, evalValid.score());

    // Plot < 50 m²
    plot.setDetailsInfo(new PropertyDetails("30 m²", 30.0, 0.0, 100_000L));
    DataQualityEngine.QualityEvaluation evalSmall = engine.evaluate(plot);
    assertTrue(evalSmall.deductions().stream().anyMatch(d -> d.contains("Irreális telekméret")));

    // Plot > 500,000 m²
    plot.setDetailsInfo(new PropertyDetails("600000 m²", 600_000.0, 0.0, 10_000L));
    DataQualityEngine.QualityEvaluation evalLarge = engine.evaluate(plot);
    assertTrue(evalLarge.deductions().stream().anyMatch(d -> d.contains("Irreális telekméret")));
  }

  @Test
  @DisplayName("Area sanity: other property types (<5 m² or >500,000 m²)")
  void testEvaluateAreaSanityOther() {
    IngatlanItem garage = createValidResidentialItem();
    garage.setPropertyType(PropertyType.GARAZS);
    garage.setPriceInfo(new PriceInfo("6 M Ft", 6_000_000L, 6.0));

    // Too small garage (< 5 m²)
    garage.setDetailsInfo(new PropertyDetails("3 m²", 3.0, 0.0, 2_000_000L));
    DataQualityEngine.QualityEvaluation evalSmall = engine.evaluate(garage);
    assertTrue(evalSmall.deductions().stream().anyMatch(d -> d.contains("Irreális alapterület")));
  }

  @Test
  @DisplayName("City presence: missing or unknown city deductions (-20)")
  void testEvaluateCityPresence() {
    IngatlanItem item = createValidResidentialItem();
    item.setCity(null);
    item.setLocation(null);

    DataQualityEngine.QualityEvaluation eval = engine.evaluate(item);
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen település")));

    // "Ismeretlen" string
    item.setCity("Ismeretlen");
    item.setLocation("Ismeretlen");
    DataQualityEngine.QualityEvaluation evalUnknown = engine.evaluate(item);
    assertTrue(evalUnknown.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen település")));
  }

  @Test
  @DisplayName("Type presence: unknown or null type deductions (-15)")
  void testEvaluateTypePresence() {
    IngatlanItem item = createValidResidentialItem();
    item.setPropertyType(PropertyType.EGYEB_ISMERETLEN);

    DataQualityEngine.QualityEvaluation eval = engine.evaluate(item);
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Ismeretlen vagy hiányzó ingatlantípus")));

    item.setPropertyType((PropertyType) null);
    DataQualityEngine.QualityEvaluation evalNull = engine.evaluate(item);
    assertTrue(evalNull.deductions().stream().anyMatch(d -> d.contains("Ismeretlen vagy hiányzó ingatlantípus")));
  }

  @Test
  @DisplayName("Price per m² plausibility: residential bounds (<100k or >4M Ft/m²)")
  void testEvaluatePricePerM2Residential() {
    // Too low price/m² (< 100,000 Ft/m²)
    IngatlanItem cheap = createValidResidentialItem();
    cheap.setPriceInfo(new PriceInfo("4 M Ft", 4_000_000L, 4.0));
    cheap.setDetailsInfo(new PropertyDetails("100 m² • 3 szoba", 100.0, 3.0, 40_000L));
    DataQualityEngine.QualityEvaluation evalCheap = engine.evaluate(cheap);
    assertTrue(evalCheap.deductions().stream().anyMatch(d -> d.contains("Irreális négyzetméterár")));

    // Too high price/m² (> 4,000,000 Ft/m²)
    IngatlanItem expensive = createValidResidentialItem();
    expensive.setPriceInfo(new PriceInfo("300 M Ft", 300_000_000L, 300.0));
    expensive.setDetailsInfo(new PropertyDetails("50 m² • 2 szoba", 50.0, 2.0, 6_000_000L));
    DataQualityEngine.QualityEvaluation evalExp = engine.evaluate(expensive);
    assertTrue(evalExp.deductions().stream().anyMatch(d -> d.contains("Irreális négyzetméterár")));
  }

  @Test
  @DisplayName("Plots are strictly exempt from residential price/m² and room density checks")
  void testEvaluatePlotExemptionFromResidentialRules() {
    IngatlanItem plot = createValidResidentialItem();
    plot.setPropertyType(PropertyType.TELEK);
    // 5 M Ft for 1000 m² = 5,000 Ft/m² (< 100,000 Ft/m²) and 0 rooms
    plot.setPriceInfo(new PriceInfo("5 M Ft", 5_000_000L, 5.0));
    plot.setDetailsInfo(new PropertyDetails("1000 m²", 1000.0, 0.0, 5_000L));

    DataQualityEngine.QualityEvaluation eval = engine.evaluate(plot);
    assertEquals(100, eval.score());
    assertTrue(eval.deductions().isEmpty());
  }

  @Test
  @DisplayName("Room density plausibility: residential area/room bounds (<12 or >150 m²/szoba)")
  void testEvaluateRoomDensity() {
    // Too dense: 40 m² with 4 rooms = 10 m²/szoba (< 12)
    IngatlanItem dense = createValidResidentialItem();
    dense.setDetailsInfo(new PropertyDetails("40 m² • 4 szoba", 40.0, 4.0, 1_000_000L));
    DataQualityEngine.QualityEvaluation evalDense = engine.evaluate(dense);
    assertTrue(evalDense.deductions().stream().anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));

    // Too sparse: 200 m² with 1 room = 200 m²/szoba (> 150)
    IngatlanItem sparse = createValidResidentialItem();
    sparse.setDetailsInfo(new PropertyDetails("200 m² • 1 szoba", 200.0, 1.0, 500_000L));
    DataQualityEngine.QualityEvaluation evalSparse = engine.evaluate(sparse);
    assertTrue(evalSparse.deductions().stream().anyMatch(d -> d.contains("Inkonzisztens szobaszám-alapterület")));
  }

  @Test
  @DisplayName("Title presence: null or blank title deduction (-10)")
  void testEvaluateTitlePresence() {
    IngatlanItem item = createValidResidentialItem();
    item.setTitle("   ");
    DataQualityEngine.QualityEvaluation eval = engine.evaluate(item);
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó hirdetéscím")));
  }

  @Test
  @DisplayName("URL validity: null, blank, or non-http/https URL deduction (-20)")
  void testEvaluateUrlValidity() {
    IngatlanItem item = createValidResidentialItem();
    item.setUrl("ftp://dh.hu/123");
    DataQualityEngine.QualityEvaluation eval = engine.evaluate(item);
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen hirdetés URL")));

    item.setUrl("");
    DataQualityEngine.QualityEvaluation evalBlank = engine.evaluate(item);
    assertTrue(evalBlank.deductions().stream().anyMatch(d -> d.contains("Hiányzó vagy érvénytelen hirdetés URL")));
  }

  @Test
  @DisplayName("Photo presence: missing or blank imageUrl deduction (-10)")
  void testEvaluatePhotoPresence() {
    IngatlanItem item = createValidResidentialItem();
    item.setImageUrl(null);
    DataQualityEngine.QualityEvaluation eval = engine.evaluate(item);
    assertTrue(eval.deductions().stream().anyMatch(d -> d.contains("Hiányzó fényképes dokumentáció")));
    assertEquals(90, eval.score());
  }

  @Test
  @DisplayName("Anomaly check: extreme price shift (>50%) deduction (-25)")
  void testEvaluatePriceVolatility() {
    IngatlanItem item = createValidResidentialItem();
    item.setPriceChangePercent(55.2);
    DataQualityEngine.QualityEvaluation evalUp = engine.evaluate(item);
    assertTrue(evalUp.deductions().stream().anyMatch(d -> d.contains("Extrém árváltozás")));
    assertEquals(75, evalUp.score());

    item.setPriceChangePercent(-62.0);
    DataQualityEngine.QualityEvaluation evalDown = engine.evaluate(item);
    assertTrue(evalDown.deductions().stream().anyMatch(d -> d.contains("Extrém árváltozás")));

    item.setPriceChangePercent(48.0);
    DataQualityEngine.QualityEvaluation evalNormal = engine.evaluate(item);
    assertFalse(evalNormal.deductions().stream().anyMatch(d -> d.contains("Extrém árváltozás")));
    assertEquals(100, evalNormal.score());
  }

  @Test
  @DisplayName("Score clamping: multiple failures clamp score strictly to [0, 100]")
  void testEvaluateScoreClamping() {
    IngatlanItem worstItem = new IngatlanItem();
    worstItem.setUrl(null); // -20
    worstItem.setTitle(null); // -10
    worstItem.setPrice(null); // -30
    worstItem.setCity(null); // -20
    worstItem.setPropertyType(PropertyType.EGYEB_ISMERETLEN); // -15
    worstItem.setImageUrl(null); // -10
    worstItem.setPriceChangePercent(80.0); // -25
    // Total deductions = 130 -> score would be -30 without clamp

    DataQualityEngine.QualityEvaluation eval = engine.evaluate(worstItem);
    assertEquals(0, eval.score());
    assertTrue(eval.deductions().size() >= 5);
  }

  @Test
  @DisplayName("applyQualityScore mutates item fields and evaluateAll processes collections")
  void testApplyQualityScoreAndEvaluateAll() {
    IngatlanItem item1 = createValidResidentialItem();
    item1.setImageUrl(null); // -10 -> 90

    IngatlanItem item2 = createValidResidentialItem();
    item2.setTitle(null); // -10
    item2.setImageUrl(null); // -10 -> 80

    engine.evaluateAll(List.of(item1, item2));

    assertEquals(90, item1.getQualityScore());
    assertEquals(1, item1.getQualityDeductions().size());

    assertEquals(80, item2.getQualityScore());
    assertEquals(2, item2.getQualityDeductions().size());
  }
}
