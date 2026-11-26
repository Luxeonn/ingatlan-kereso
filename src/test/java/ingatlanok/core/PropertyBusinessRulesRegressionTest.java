package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.MarketStats;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.model.SourceListing;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.testsupport.TestFixtures;

/**
 * Deterministic business logic & ranking regression test suite (#375).
 * Validates domain rules independently from scrapers and databases:
 * - Price normalization & currency/unit parsing
 * - Ft/m² calculation, rounding, zero/missing inputs
 * - Criteria filtering: price, area, city, and view-state combinations
 * - Bargain detection & data quality scoring preconditions
 * - Deterministic sorting and tie-breaking order (price, new-state, discovered date, url)
 * - New-construction classification & cross-portal consolidation
 * - Malformed, contradictory, zero/negative, and boundary values
 */
@DisplayName("Ingatlan Üzleti Szabályok és Rangsorolás Regressziós Tesztek (#375)")
class PropertyBusinessRulesRegressionTest {

  @Nested
  @DisplayName("1. Árnormalizálás, Pénznem és Mértékegység Kezelés")
  class PriceNormalizationAndUnitTests {

    @Test
    @DisplayName("Millió Ft formátumok (vessző, pont, szóköz, kis/nagybetű) feldolgozása")
    void testParsePriceMillionFormats() {
      PriceInfo p1 = PropertyParser.parsePrice("Eladó lakás 65.5 M Ft");
      assertEquals(65_500_000L, p1.amountHuf());
      assertEquals(65.5, p1.amountMillion());

      PriceInfo p2 = PropertyParser.parsePrice("Ikerház 65,5 M Ft");
      assertEquals(65_500_000L, p2.amountHuf());
      assertEquals(65.5, p2.amountMillion());

      PriceInfo p3 = PropertyParser.parsePrice("Családi ház 120 m ft");
      assertEquals(120_000_000L, p3.amountHuf());
      assertEquals(120.0, p3.amountMillion());

      PriceInfo p4 = PropertyParser.parsePrice("Telek 8.25 M Ft irányár");
      assertEquals(8_250_000L, p4.amountHuf());
      assertEquals(8.25, p4.amountMillion());
    }

    @Test
    @DisplayName("Egész Forint formátumok (szóköz, pont, NBSP tagolás) feldolgozása")
    void testParsePriceIntegerHufFormats() {
      PriceInfo pSpace = PropertyParser.parsePrice("Lakás 45 500 000 Ft");
      assertEquals(45_500_000L, pSpace.amountHuf());
      assertEquals(45.5, pSpace.amountMillion());

      PriceInfo pDot = PropertyParser.parsePrice("Családi ház 78.900.000 Ft");
      assertEquals(78_900_000L, pDot.amountHuf());
      assertEquals(78.9, pDot.amountMillion());

      PriceInfo pNbsp = PropertyParser.parsePrice("Panel 32\u00A0400\u00A0000 Ft");
      assertEquals(32_400_000L, pNbsp.amountHuf());
      assertEquals(32.4, pNbsp.amountMillion());
    }

    @Test
    @DisplayName("Null, üres, nulla vagy értelmezhetetlen ár bemenetek kezelése")
    void testParsePriceMalformedAndBoundaryInputs() {
      PriceInfo pNull = PropertyParser.parsePrice(null);
      assertNotNull(pNull);
      assertNull(pNull.amountHuf());
      assertEquals("Ár a hirdetésben", pNull.rawText());

      PriceInfo pBlank = PropertyParser.parsePrice("   ");
      assertNull(pBlank.amountHuf());
      assertEquals("Ár a hirdetésben", pBlank.rawText());

      PriceInfo pZero = PropertyParser.parsePrice("0 Ft");
      assertNull(pZero.amountHuf(), "0 Ft amount must remain unparsed amountHuf=null");

      PriceInfo pNoPrice = PropertyParser.parsePrice("Ár megegyezés szerint, hívjon bizalommal!");
      assertNull(pNoPrice.amountHuf());
      assertEquals("Ár a hirdetésben", pNoPrice.rawText());
    }
  }

  @Nested
  @DisplayName("2. Négyzetméterár (Ft/m²) Számítás, Kerekítés és Hiányzó Adatok")
  class SquareMeterPriceCalculationTests {

    @Test
    @DisplayName("Normál m² ár egészre kerekítése felező szabály szerint")
    void testCalculateSquareMeterPriceRounding() {
      PriceInfo price50M = PriceInfo.builder().amountHuf(50_000_000L).amountMillion(50.0).build();
      PropertyDetails d1 = PropertyParser.parseDetails("75 m² 3 szoba", price50M);
      assertEquals(666_667L, d1.pricePerSquareMeter());

      PriceInfo price40M = PriceInfo.builder().amountHuf(40_000_000L).amountMillion(40.0).build();
      PropertyDetails d2 = PropertyParser.parseDetails("50 m² 2 szoba", price40M);
      assertEquals(800_000L, d2.pricePerSquareMeter());
    }

    @Test
    @DisplayName("Hiányzó, nulla vagy érvénytelen ár/terület esetén a m² ár null marad")
    void testCalculateSquareMeterPriceMissingInputs() {
      PriceInfo validPrice = PriceInfo.builder().amountHuf(45_000_000L).amountMillion(45.0).build();

      PropertyDetails dNoArea = PropertyParser.parseDetails("Csak leírás alapterület nélkül", validPrice);
      assertNull(dNoArea.areaSquareMeters());
      assertNull(dNoArea.pricePerSquareMeter());

      PropertyDetails dNullPrice = PropertyParser.parseDetails("60 m²", null);
      assertEquals(60.0, dNullPrice.areaSquareMeters());
      assertNull(dNullPrice.pricePerSquareMeter());

      PriceInfo zeroPrice = PriceInfo.builder().amountHuf(0L).amountMillion(0.0).build();
      PropertyDetails dZeroPrice = PropertyParser.parseDetails("60 m²", zeroPrice);
      assertNull(dZeroPrice.pricePerSquareMeter());
    }

    @Test
    @DisplayName("Különböző mértékegység jelölések (m², m2, nm, négyzetméter) kinyerése")
    void testAreaUnitVariantsExtraction() {
      PriceInfo price = PriceInfo.builder().amountHuf(30_000_000L).amountMillion(30.0).build();

      PropertyDetails dM2 = PropertyParser.parseDetails("45.5 m2 eladó", price);
      assertEquals(45.5, dM2.areaSquareMeters());

      PropertyDetails dNm = PropertyParser.parseDetails("52 nm-es lakás", price);
      assertEquals(52.0, dNm.areaSquareMeters());

      PropertyDetails dFull = PropertyParser.parseDetails("120 négyzetméter tégla ház", price);
      assertEquals(120.0, dFull.areaSquareMeters());
    }
  }

  @Nested
  @DisplayName("3. Szűrési Szabályok: Ár, Terület, Település és Nézet Feltételek")
  class FilteringRuleTests {

    private final PropertyFilterEngine filterEngine = new PropertyFilterEngine();

    @Test
    @DisplayName("Ár és alapterület határfeltételek (inkluzív tűrés ±0.001)")
    void testPriceAndAreaRangeInclusiveBoundaries() {
      SearchCriteria criteria = new SearchCriteria();
      criteria.setMinPriceMillion(40);
      criteria.setMaxPriceMillion(60);
      criteria.setMinArea(50);
      criteria.setMaxArea(100);

      IngatlanItem exactMin = TestFixtures.validResidentialItem("EX-MIN", "Debrecen", 40_000_000L, 50.0, 2.0);
      IngatlanItem exactMax = TestFixtures.validResidentialItem("EX-MAX", "Debrecen", 60_000_000L, 100.0, 4.0);
      IngatlanItem justBelowPrice = TestFixtures.validResidentialItem("BELOW-P", "Debrecen", 39_900_000L, 60.0, 2.0);
      IngatlanItem justAbovePrice = TestFixtures.validResidentialItem("ABOVE-P", "Debrecen", 60_100_000L, 60.0, 2.0);
      IngatlanItem justBelowArea = TestFixtures.validResidentialItem("BELOW-A", "Debrecen", 50_000_000L, 49.5, 2.0);
      IngatlanItem justAboveArea = TestFixtures.validResidentialItem("ABOVE-A", "Debrecen", 50_000_000L, 100.5, 4.0);

      List<IngatlanItem> items = List.of(
          exactMin, exactMax, justBelowPrice, justAbovePrice, justBelowArea, justAboveArea);

      PropertyFilterEngine.FilterResult result = filterEngine.filter(items, criteria, "Debrecen");
      assertEquals(2, result.matchingItems().size());
      assertTrue(result.matchingItems().contains(exactMin));
      assertTrue(result.matchingItems().contains(exactMax));
      assertEquals(2, result.filteredByPriceCount());
      assertEquals(2, result.filteredByAreaCount());
    }

    @Test
    @DisplayName("Kombinált nézet szűrők: excludeViewed, onlyDiscounted, onlyNewConstruction")
    void testCombinedViewFilters() {
      SearchCriteria criteria = new SearchCriteria();
      criteria.setMinPriceMillion(30);
      criteria.setMaxPriceMillion(70);
      criteria.setMinArea(40);
      criteria.setMaxArea(120);
      criteria.setExcludeViewed(true);
      criteria.setOnlyDiscounted(true);
      criteria.setOnlyNewConstruction(true);

      IngatlanItem matchAll = TestFixtures.validResidentialItem("M1", "Debrecen", 50_000_000L, 60.0, 2.0);
      matchAll.setViewed(false);
      matchAll.setIsBargain(true);
      matchAll.setPriceDelta(-2_000_000L);
      matchAll.setNewConstruction(true);

      IngatlanItem viewedItem = TestFixtures.validResidentialItem("M2", "Debrecen", 50_000_000L, 60.0, 2.0);
      viewedItem.setViewed(true);
      viewedItem.setIsBargain(true);
      viewedItem.setPriceDelta(-2_000_000L);
      viewedItem.setNewConstruction(true);

      IngatlanItem nonDiscountedItem = TestFixtures.validResidentialItem("M3", "Debrecen", 50_000_000L, 60.0, 2.0);
      nonDiscountedItem.setViewed(false);
      nonDiscountedItem.setPriceDelta(null);
      nonDiscountedItem.setNewConstruction(true);

      IngatlanItem notNewConstItem = TestFixtures.validResidentialItem("M4", "Debrecen", 50_000_000L, 60.0, 2.0);
      notNewConstItem.setViewed(false);
      notNewConstItem.setPriceDelta(-1_000_000L);
      notNewConstItem.setNewConstruction(false);

      List<IngatlanItem> items = List.of(matchAll, viewedItem, nonDiscountedItem, notNewConstItem);
      PropertyFilterEngine.FilterResult result = filterEngine.filter(items, criteria, "Debrecen");

      assertEquals(1, result.matchingItems().size());
      assertEquals("M1", result.matchingItems().get(0).getId());
      assertEquals(1, result.filteredByViewedCount());
      assertEquals(1, result.filteredByDiscountCount());
      assertEquals(1, result.filteredByNewConstructionCount());
    }

    @Test
    @DisplayName("Város egyezés slug és részsztring alapján, illetve eltérő város kizárása")
    void testCityMatchingSemantics() {
      IngatlanItem debrecenItem = TestFixtures.validResidentialItem("C1", "Debrecen", 50_000_000L, 60.0, 2.0);
      IngatlanItem szegedItem = TestFixtures.validResidentialItem("C2", "Szeged", 50_000_000L, 60.0, 2.0);
      IngatlanItem accentedItem = TestFixtures.validResidentialItem("C3", "Hajdúszoboszló", 50_000_000L, 60.0, 2.0);

      assertTrue(SearchCriteria.matchesCity(debrecenItem, "Debrecen"));
      assertFalse(SearchCriteria.matchesCity(szegedItem, "Debrecen"));
      assertTrue(SearchCriteria.matchesCity(accentedItem, "hajduszoboszlo"));
    }
  }

  @Nested
  @DisplayName("4. Alkalmi Vétel (Bargain) és Minőségi Pontszám Szabályok")
  class BargainAndQualityRulesTests {

    private final MarketAnalyticsEngine analyticsEngine = TestFixtures.marketAnalyticsEngine();

    @Test
    @DisplayName("Alkalmi vétel szigorú 20%-os küszöbértéke a mediánhoz viszonyítva")
    void testBargainThresholdExactBoundary() {
      List<IngatlanItem> items = new ArrayList<>();
      for (int i = 1; i <= 5; i++) {
        items.add(TestFixtures.validResidentialItem("REF-" + i, "Szeged", 50_000_000L, 50.0, 2.0));
      }

      IngatlanItem exact20Bargain =
          TestFixtures.validResidentialItem("BARGAIN-20", "Szeged", 40_000_000L, 50.0, 2.0);
      items.add(exact20Bargain);

      IngatlanItem notBargain =
          TestFixtures.validResidentialItem("NOT-BARGAIN", "Szeged", 40_030_000L, 50.0, 2.0);
      items.add(notBargain);

      MarketStats stats = analyticsEngine.calculateMarketStats(items);
      assertNotNull(stats);

      assertTrue(exact20Bargain.getIsBargain(), "Pontosan 20.0% engedmény esetén alkalmi vételnek kell lennie");
      assertEquals(20.0, exact20Bargain.getPercentageBelowMedian());

      assertFalse(notBargain.getIsBargain(), "20% alatti engedmény (19.94%) nem lehet alkalmi vétel");
      assertEquals(19.9, notBargain.getPercentageBelowMedian());
    }

    @Test
    @DisplayName("Minőségi pontszám < 70 esetén az ingatlan nem lehet alkalmi vétel és mediánt sem képezhet")
    void testLowQualityItemQuarantinedFromMedianAndBargain() {
      List<IngatlanItem> items = new ArrayList<>();
      for (int i = 1; i <= 5; i++) {
        items.add(TestFixtures.validItemWithQuality(
            "SZ-PEER-" + i, "Szeged", PropertyType.LAKAS, 40_000_000L, 50.0, 95));
      }

      IngatlanItem lowQualityCheap =
          TestFixtures.validItemWithQuality("LOW-QUAL", "Szeged", PropertyType.LAKAS, 10_000_000L, 50.0, 45);
      items.add(lowQualityCheap);

      analyticsEngine.calculateMarketStats(items);
      assertFalse(lowQualityCheap.getIsBargain(),
          "Karanténozott (< 70 pont) elem nem kaphat isBargain=true minősítést");
      assertNull(lowQualityCheap.getReferenceMedianPricePerSquareMeter());
    }

    @Test
    @DisplayName("Elégtelen minta (N < 5) esetén nincs medián és nincs alkalmi vétel")
    void testInsufficientSampleSizeSuppressesBargain() {
      List<IngatlanItem> smallSample = List.of(
          TestFixtures.validResidentialItem("S1", "Kisvaros", 30_000_000L, 50.0, 2.0),
          TestFixtures.validResidentialItem("S2", "Kisvaros", 20_000_000L, 50.0, 2.0),
          TestFixtures.validResidentialItem("S3", "Kisvaros", 35_000_000L, 50.0, 2.0)
      );

      MarketStats stats = analyticsEngine.calculateMarketStats(smallSample);
      assertNull(stats.residentialMedianPricePerM2());
      assertTrue(stats.residentialInsufficientData());
      assertEquals(0, stats.bargainCount());

      for (IngatlanItem it : smallSample) {
        assertFalse(it.getIsBargain());
        assertNull(it.getReferenceMedianPricePerSquareMeter());
      }
    }
  }

  @Nested
  @DisplayName("5. Rendezési és Rangsorolási Szabályok, Tie-Breakerek")
  class SortingAndTieBreakerTests {

    private final SearchResultAggregator aggregator = new SearchResultAggregator();

    @Test
    @DisplayName("Alapértelmezett rendezés: ár növekvő -> új (isNew) előre -> felfedezés ideje csökkenő -> URL")
    void testDefaultRankingAndTieBreakerOrder() {
      IngatlanItem cheap = TestFixtures.validResidentialItem("A-CHEAP", "Debrecen", 30_000_000L, 50.0, 2.0);
      cheap.setUrl("https://dh.hu/cheap");
      cheap.setNew(false);
      cheap.setDiscoveredAt("2026-03-01 10:00:00");

      IngatlanItem midNewRecent =
          TestFixtures.validResidentialItem("B-MID-NEW-RECENT", "Debrecen", 40_000_000L, 50.0, 2.0);
      midNewRecent.setUrl("https://dh.hu/mid-recent");
      midNewRecent.setNew(true);
      midNewRecent.setDiscoveredAt("2026-03-02 12:00:00");

      IngatlanItem midNewOlder =
          TestFixtures.validResidentialItem("C-MID-NEW-OLDER", "Debrecen", 40_000_000L, 50.0, 2.0);
      midNewOlder.setUrl("https://dh.hu/mid-older");
      midNewOlder.setNew(true);
      midNewOlder.setDiscoveredAt("2026-03-01 08:00:00");

      IngatlanItem midOld = TestFixtures.validResidentialItem("D-MID-OLD", "Debrecen", 40_000_000L, 50.0, 2.0);
      midOld.setUrl("https://dh.hu/mid-old");
      midOld.setNew(false);
      midOld.setDiscoveredAt("2026-03-02 15:00:00");

      IngatlanItem expensive = TestFixtures.validResidentialItem("E-EXPENSIVE", "Debrecen", 50_000_000L, 50.0, 2.0);
      expensive.setUrl("https://dh.hu/expensive");
      expensive.setNew(true);
      expensive.setDiscoveredAt("2026-03-02 10:00:00");

      List<IngatlanItem> items = new ArrayList<>(List.of(
          expensive, midOld, midNewOlder, midNewRecent, cheap));

      SearchResult result = aggregator.aggregate(items, new SearchCriteria(), null, null, null, null);
      List<IngatlanItem> sorted = result.getItems();

      assertEquals("A-CHEAP", sorted.get(0).getId());
      assertEquals("B-MID-NEW-RECENT", sorted.get(1).getId());
      assertEquals("C-MID-NEW-OLDER", sorted.get(2).getId());
      assertEquals("D-MID-OLD", sorted.get(3).getId());
      assertEquals("E-EXPENSIVE", sorted.get(4).getId());
    }

    @Test
    @DisplayName("Teljesen azonos ár, új-státusz és dátum esetén az URL tie-breaker determinisztikus sorrendet ad")
    void testIdenticalMetricsDeterministicUrlTieBreaker() {
      IngatlanItem itemBeta = TestFixtures.validResidentialItem("TIE-B", "Debrecen", 40_000_000L, 50.0, 2.0);
      itemBeta.setUrl("https://portal.hu/listing-beta");
      itemBeta.setNew(true);
      itemBeta.setDiscoveredAt("2026-03-01 10:00:00");

      IngatlanItem itemAlpha = TestFixtures.validResidentialItem("TIE-A", "Debrecen", 40_000_000L, 50.0, 2.0);
      itemAlpha.setUrl("https://portal.hu/listing-alpha");
      itemAlpha.setNew(true);
      itemAlpha.setDiscoveredAt("2026-03-01 10:00:00");

      List<IngatlanItem> run1 = new ArrayList<>(List.of(itemBeta, itemAlpha));
      aggregator.aggregate(run1, new SearchCriteria(), null, null, null, null);

      List<IngatlanItem> run2 = new ArrayList<>(List.of(itemAlpha, itemBeta));
      aggregator.aggregate(run2, new SearchCriteria(), null, null, null, null);

      assertEquals("TIE-A", run1.get(0).getId());
      assertEquals("TIE-B", run1.get(1).getId());
      assertEquals("TIE-A", run2.get(0).getId());
      assertEquals("TIE-B", run2.get(1).getId());
    }
  }

  @Nested
  @DisplayName("6. Új Építésű Besorolás és Többforrásos Kluszterezés")
  class NewConstructionAndMultiSourceTests {

    private final DeduplicationEngine dedupEngine = TestFixtures.deduplicationEngine();
    private final MarketAnalyticsEngine analyticsEngine = TestFixtures.marketAnalyticsEngine();

    @Test
    @DisplayName("Új építésű kulcsszavak azonosítása címben, leírásban és URL-ben")
    void testNewConstructionKeywordDetection() {
      assertTrue(PropertyParser.isNewConstructionText("Új építésű prémium lakás"));
      assertTrue(PropertyParser.isNewConstructionText("Szerkezetkész ikerház eladó"));
      assertTrue(PropertyParser.isNewConstructionText("Lakópark projekt 2026"));
      assertTrue(PropertyParser.isNewConstructionText("https://dh.hu/ingatlan/uj-epitesu-lakas"));

      assertFalse(PropertyParser.isNewConstructionText("Felújított polgári téglalakás"));
      assertFalse(PropertyParser.isNewConstructionText((String[]) null));
    }

    @Test
    @DisplayName("Több forrásból származó hirdetés kluszterezése a legalacsonyabb árat választja és forráslistát épít")
    void testCrossPortalClusterSelectsLowestPriceAndBuildsSources() {
      IngatlanItem dhItem = TestFixtures.validResidentialItem("DH-1", "Debrecen", 42_000_000L, 60.0, 2.0);
      dhItem.setUrl("https://dh.hu/ingatlan/1");
      dhItem.setTitle("Debrecen csendes utcájában lakás");
      dhItem.setNewConstruction(false);

      IngatlanItem ocItem = TestFixtures.validResidentialItem("OC-1", "Debrecen", 39_500_000L, 60.0, 2.0);
      ocItem.setUrl("https://oc.hu/ingatlan/1");
      ocItem.setTitle("Debrecen csendes utcájában lakás");
      ocItem.setNewConstruction(true);

      List<IngatlanItem> clustered = dedupEngine.deduplicateMultiSignal(List.of(dhItem, ocItem));
      assertEquals(1, clustered.size(), "A két azonos fizikai ingatlannak 1 kártyába kell összeolvadnia");

      IngatlanItem primary = clustered.get(0);
      assertEquals("OC-1", primary.getId(), "A legalacsonyabb aktív árú forrás (39.5M) a megjelenített elsődleges");
      assertEquals(39_500_000L, primary.getPriceInfo().amountHuf());
      assertTrue(primary.isNewConstruction(), "Az új építésű jelölést örökölnie kell a kluszter tagjától");
      assertEquals(2, primary.getSourcesCount());

      List<SourceListing> sources = primary.getSourceListings();
      assertNotNull(sources);
      assertEquals(2, sources.size());
      assertEquals(39_500_000L, sources.get(0).getPriceHuf());
      assertEquals(42_000_000L, sources.get(1).getPriceHuf());
    }

    @Test
    @DisplayName("Többforrásos kluszterbe olvadt másodlagos portál is megtartja a SUCCESS státuszt és számlálást")
    void testSecondaryPortalInMergedClusterRetainsSuccessStatus() {
      IngatlanItem ocPrimary = TestFixtures.validResidentialItem("OC-1", "Debrecen", 39_500_000L, 60.0, 2.0);
      ocPrimary.setPortal("Otthon Centrum");
      ocPrimary.setSource(PortalSource.of("Otthon Centrum"));
      ocPrimary.setSourcesCount(2);
      ocPrimary.setSourceListings(List.of(
          SourceListing.builder().portal("Otthon Centrum").url("https://oc.hu/1").priceHuf(39_500_000L).build(),
          SourceListing.builder().portal("Duna House").url("https://dh.hu/1").priceHuf(42_000_000L).build()));

      SearchPersistenceSynchronizer sync = new SearchPersistenceSynchronizer(
          new PortalEngineRegistry(List.of(
              TestFixtures.dhEngine(),
              TestFixtures.ocEngine())),
          Mockito.mock(ListingStorage.class));

      Map<String, Integer> counts = new LinkedHashMap<>();
      counts.put("Duna House", 0);
      counts.put("Otthon Centrum", 0);
      sync.recalculateRelevantPortalCounts(counts, List.of(ocPrimary));
      assertEquals(0, counts.get("Duna House"), "A másodlagos forrás nem duplázhatja a nettó portál-számlálót (#432)");
      assertEquals(1, counts.get("Otthon Centrum"));

      List<PortalScrapeResult> rawResults = List.of(
          PortalScrapeResult.builder()
              .portalId("dh")
              .portalName("Duna House")
              .status(PortalScrapeStatus.SUCCESS)
              .items(List.of(ocPrimary))
              .build());
      List<PortalScrapeResult> agg =
          sync.aggregatePortalResultsByPortal(rawResults, List.of(ocPrimary));
      assertEquals(1, agg.size());
      assertEquals(PortalScrapeStatus.SUCCESS, agg.get(0).getStatus(),
          "A másodlagos forrásként beolvadt portál státusza nem eshet vissza EMPTY-re");
    }

    @Test
    @DisplayName(
        "Rejtett (hidden=true) elemek nem torzítják a totalFound, newCount, portalCounts,"
            + " cityCounts és MarketStats mutatókat (#502)")
    void testHiddenItemsExcludedFromActiveCountsAndMarketAnalytics() {
      List<IngatlanItem> mixed = new ArrayList<>();
      for (int i = 1; i <= 5; i++) {
        IngatlanItem active =
            TestFixtures.validResidentialItem("ACT-" + i, "Debrecen", 50_000_000L, 50.0, 2.0);
        active.setUrl("https://dh.hu/act-" + i);
        active.setPortal("Duna House");
        active.setSource(PortalSource.of("Duna House"));
        active.setNew(i <= 2);
        active.setHidden(false);
        mixed.add(active);
      }

      // Active bargain (750k Ft/m2 -> 25% below 1M median)
      IngatlanItem activeBargain =
          TestFixtures.validResidentialItem("ACT-BARGAIN", "Debrecen", 37_500_000L, 50.0, 2.0);
      activeBargain.setUrl("https://oc.hu/act-bargain");
      activeBargain.setPortal("Otthon Centrum");
      activeBargain.setSource(PortalSource.of("Otthon Centrum"));
      activeBargain.setNew(true);
      activeBargain.setHidden(false);
      mixed.add(activeBargain);

      // Hidden item with very cheap price (would be a bargain and lower median/avg if counted) and isNew=true
      IngatlanItem hiddenBargain =
          TestFixtures.validResidentialItem("HID-BARGAIN", "Debrecen", 25_000_000L, 50.0, 2.0);
      hiddenBargain.setUrl("https://dh.hu/hid-bargain");
      hiddenBargain.setPortal("Duna House");
      hiddenBargain.setSource(PortalSource.of("Duna House"));
      hiddenBargain.setNew(true);
      hiddenBargain.setHidden(true);
      hiddenBargain.setPriceDelta(-5_000_000L);
      hiddenBargain.setPriceChangePercent(-16.7);
      mixed.add(hiddenBargain);

      // 1. MarketAnalyticsEngine excludes hidden item from counts, medians, bargains, and price drops
      MarketStats stats = analyticsEngine.calculateMarketStats(mixed);
      assertEquals(6, stats.residentialSampleSize());
      assertEquals(1_000_000L, stats.residentialMedianPricePerM2());
      assertEquals(1, stats.bargainCount(), "Hidden item must not be counted in bargainCount");
      assertEquals(0, stats.priceDropCount(), "Hidden item must not be counted in priceDropCount");
      assertEquals(6, stats.cityBreakdown().get("Debrecen"));
      assertFalse(hiddenBargain.getIsBargain(), "Hidden item must not keep isBargain=true");
      assertNull(hiddenBargain.getPercentageBelowMedian());

      // 2. SearchResultAggregator counts only non-hidden items in totalFound and newCount while
      // retaining hidden items in list
      Map<String, Integer> portalCounts = new LinkedHashMap<>();
      portalCounts.put("Duna House", 0);
      portalCounts.put("Otthon Centrum", 0);
      SearchPersistenceSynchronizer sync =
          new SearchPersistenceSynchronizer(
              new PortalEngineRegistry(List.of(TestFixtures.dhEngine(), TestFixtures.ocEngine())),
              Mockito.mock(ListingStorage.class));
      sync.recalculateRelevantPortalCounts(portalCounts, mixed);
      assertEquals(5, portalCounts.get("Duna House"), "Hidden DH item must not inflate portalCounts");
      assertEquals(1, portalCounts.get("Otthon Centrum"));

      SearchResultAggregator agg = new SearchResultAggregator();
      SearchResult result =
          agg.aggregate(new ArrayList<>(mixed), new SearchCriteria(), null, portalCounts, null, null);
      assertEquals(7, result.getItems().size(), "Hidden items remain in items for Trash/ShowHidden UI");
      assertEquals(6, result.getTotalFound(), "totalFound must count only active (!hidden) items");
      assertEquals(3, result.getNewCount(), "newCount must count only active (!hidden) new items");
    }

    @Test
    @DisplayName("Alkalmi vétel 19.96% feletti kerekítési rés nélkül és multi-source Ft/m² összhanggal (#502)")
    void testBargainRoundingGapAndMultiSourceSqmPriceAlignment() {
      List<IngatlanItem> items = new ArrayList<>();
      for (int i = 1; i <= 5; i++) {
        items.add(TestFixtures.validResidentialItem("M-" + i, "Debrecen", 50_000_000L, 50.0, 2.0));
      }
      // 19.96% below 1,000,000 -> 800,400 Ft/m² (40,020,000 Ft / 50 m²)
      // Rounds to 20.0 at 1 decimal -> must be consistent between isBargain and percentageBelowMedian
      IngatlanItem edgeItem =
          TestFixtures.validResidentialItem("EDGE-1996", "Debrecen", 40_020_000L, 50.0, 2.0);
      items.add(edgeItem);

      MarketStats stats = analyticsEngine.calculateMarketStats(items);
      assertEquals(20.0, edgeItem.getPercentageBelowMedian());
      assertTrue(edgeItem.getIsBargain(),
          "19.96% rounds to 20.0% display value -> isBargain must match percentageBelowMedian >= 20.0");
      assertEquals(1, stats.bargainCount());
    }
  }

  @Nested
  @DisplayName("7. Határértékek, Nullok és Ellentmondó Rekordok Kezelése")
  class EdgeCasesAndConflictTests {

    private final DeduplicationEngine dedupEngine = TestFixtures.deduplicationEngine();
    private final MarketAnalyticsEngine analyticsEngine = TestFixtures.marketAnalyticsEngine();

    @Test
    @DisplayName("Ellentmondó adatok (pl. eltérő szobaszám vagy túl nagy árkülönbség) nem vonhatók össze")
    void testConflictingRecordsDoNotCluster() {
      IngatlanItem smallRooms = TestFixtures.validResidentialItem("R1", "Debrecen", 40_000_000L, 60.0, 1.0);
      IngatlanItem largeRooms = TestFixtures.validResidentialItem("R2", "Debrecen", 40_000_000L, 60.0, 4.0);

      List<IngatlanItem> clusters = dedupEngine.deduplicateMultiSignal(List.of(smallRooms, largeRooms));
      assertEquals(2, clusters.size(), "Jelentős szobaszám különbség esetén nem kluszterezhetők");

      IngatlanItem item40M = TestFixtures.validResidentialItem("P1", "Debrecen", 40_000_000L, 60.0, 2.0);
      IngatlanItem item55M = TestFixtures.validResidentialItem("P2", "Debrecen", 55_000_000L, 60.0, 2.0);

      List<IngatlanItem> priceClusters = dedupEngine.deduplicateMultiSignal(List.of(item40M, item55M));
      assertEquals(2, priceClusters.size(), "20%-nál nagyobb árkülönbség (>25%) kizárja az összevonást");
    }

    @Test
    @DisplayName("Üres gyűjtemények és null elemek robusztus kezelése 0-összeomlással")
    void testNullAndEmptyCollectionsSafety() {
      assertEquals(0, dedupEngine.deduplicateMultiSignal(null).size());
      assertEquals(0, dedupEngine.deduplicateMultiSignal(List.of()).size());

      List<IngatlanItem> listWithNulls = new ArrayList<>();
      listWithNulls.add(null);
      listWithNulls.add(TestFixtures.validResidentialItem("OK1", "Debrecen", 40_000_000L, 50.0, 2.0));
      listWithNulls.add(null);

      List<IngatlanItem> cleaned = dedupEngine.deduplicateMultiSignal(listWithNulls);
      assertEquals(1, cleaned.size());
      assertEquals("OK1", cleaned.get(0).getId());
    }
  }
}
