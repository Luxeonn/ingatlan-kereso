package ingatlanok.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ingatlanok.core.DataQualityEngine;
import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.IngatlanComHtmlImportService;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.MarketStatisticsCalculator;
import ingatlanok.core.ParallelPortalScrapeExecutor;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.core.SearchPersistenceSynchronizer;
import ingatlanok.core.SearchResultAggregator;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.core.normalization.ListingValidator;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.RawListingNormalizer;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DedupPresentationService;

@SpringBootTest
@DisplayName("ComponentScanWiringIntegrationTest - Spring automatikus DI gráf és singleton ellenőrzése (#472)")
class ComponentScanWiringIntegrationTest {

  @Autowired private ListingValidator listingValidator;
  @Autowired private ListingNormalizer listingNormalizer;
  @Autowired private List<PortalEngine> portalEngines;
  @Autowired private PortalEngineRegistry portalEngineRegistry;
  @Autowired private DunaHouseUrlBuilder dhUrlBuilder;
  @Autowired private DunaHousePortalEngine dhPortalEngine;
  @Autowired private OtthonCentrumUrlBuilder ocUrlBuilder;
  @Autowired private OtthonCentrumPortalEngine ocPortalEngine;
  @Autowired private JofogasUrlBuilder jofogasUrlBuilder;
  @Autowired private JofogasPortalEngine jofogasPortalEngine;
  @Autowired private IngatlanBazarQueryBuilder ingatlanBazarQueryBuilder;
  @Autowired private IngatlanBazarPortalEngine ingatlanBazarPortalEngine;
  @Autowired private KoltozzbeUrlBuilder koltozzbeUrlBuilder;
  @Autowired private KoltozzbePortalEngine koltozzbePortalEngine;
  @Autowired private IngatlanComUrlBuilder ingatlanComUrlBuilder;
  @Autowired private IngatlanComPortalEngine ingatlanComPortalEngine;
  @Autowired private DataQualityEngine dataQualityEngine;
  @Autowired private PropertyFilterEngine propertyFilterEngine;
  @Autowired private MarketStatisticsCalculator marketStatisticsCalculator;
  @Autowired private MarketAnalyticsEngine marketAnalyticsEngine;
  @Autowired private DeduplicationEngine deduplicationEngine;
  @Autowired private ParallelPortalScrapeExecutor scrapeExecutor;
  @Autowired private SearchResultAggregator resultAggregator;
  @Autowired private SearchPersistenceSynchronizer persistenceSynchronizer;
  @Autowired private SearchExecutionEngine searchExecutionEngine;
  @Autowired private IngatlanComHtmlImportService htmlImportService;
  @Autowired private DashboardPresentationService dashboardPresentationService;
  @Autowired private DedupPresentationService dedupPresentationService;
  @Autowired private ListingStorage listingStorage;
  @Autowired private ConfigStorage configStorage;

  @Test
  @DisplayName("Minden 6 portálmotor és URL builder automatikusan felderítve, közös normalizerrel")
  void testPortalEnginesAutoDiscoveredWithSharedNormalizer() {
    assertEquals(6, portalEngines.size());
    List<String> registeredIds =
        portalEngineRegistry.getAllDescriptors().stream().map(PortalDescriptor::id).toList();
    assertEquals(
        List.of("dh", "ingatlanbazar", "ingatlancom", "jofogas", "koltozzbe", "oc"),
        registeredIds);

    assertSame(dhUrlBuilder, dhPortalEngine.getUrlBuilder());
    assertSame(ocUrlBuilder, ocPortalEngine.getUrlBuilder());
    assertSame(jofogasUrlBuilder, jofogasPortalEngine.getUrlBuilder());
    assertSame(ingatlanBazarQueryBuilder, ingatlanBazarPortalEngine.getQueryBuilder());
    assertSame(koltozzbeUrlBuilder, koltozzbePortalEngine.getUrlBuilder());
    assertSame(ingatlanComUrlBuilder, ingatlanComPortalEngine.getUrlBuilder());

    for (PortalEngine engine : portalEngines) {
      if (engine instanceof AbstractAdapterPortalEngine adapterEngine) {
        assertSame(
            listingNormalizer,
            adapterEngine.getNormalizer(),
            "Minden portálmotornak a Spring singleton ListingNormalizer-t kell megkapnia");
      }
    }
  }

  @Test
  @DisplayName("RawListingNormalizer.getDefault() a Spring singletonra mutat és offline példányosítás nem írja felül")
  void testPostConstructDefaultNormalizerIsolation() {
    assertSame(listingValidator, listingNormalizer.getValidator());
    assertSame(listingNormalizer, RawListingNormalizer.getDefault());

    ListingNormalizer offlineInstance = new ListingNormalizer();
    assertNotNull(offlineInstance);
    assertSame(listingNormalizer, RawListingNormalizer.getDefault());
  }

  @Test
  @DisplayName("SearchExecutionEngine és minden segéd-komponens a közös singleton beanekkel van bekötve")
  void testCoreAndPresentationSingletonWiring() {
    assertSame(portalEngineRegistry, searchExecutionEngine.getPortalRegistry());
    assertSame(listingStorage, searchExecutionEngine.getListingStorage());
    assertSame(configStorage, searchExecutionEngine.getConfigStorage());
    assertSame(propertyFilterEngine, searchExecutionEngine.getFilterEngine());
    assertSame(deduplicationEngine, searchExecutionEngine.getDedupEngine());
    assertSame(marketAnalyticsEngine, searchExecutionEngine.getAnalyticsEngine());
    assertSame(dataQualityEngine, searchExecutionEngine.getQualityEngine());
    assertSame(scrapeExecutor, searchExecutionEngine.getScrapeExecutor());
    assertSame(resultAggregator, searchExecutionEngine.getResultAggregator());
    assertSame(persistenceSynchronizer, searchExecutionEngine.getPersistenceSynchronizer());

    assertNotNull(marketStatisticsCalculator);
    assertNotNull(htmlImportService);
    assertNotNull(dashboardPresentationService);
    assertNotNull(dedupPresentationService);
  }
}
