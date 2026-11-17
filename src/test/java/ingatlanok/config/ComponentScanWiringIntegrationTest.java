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
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.RawListingNormalizer;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DedupPresentationService;
import ingatlanok.service.IngatlanSearchService;

@SpringBootTest
@DisplayName("ComponentScanWiringIntegrationTest - Spring automatikus DI gráf és singleton ellenőrzése (#472)")
class ComponentScanWiringIntegrationTest {

  @Autowired private ListingNormalizer listingNormalizer;
  @Autowired private List<PortalEngine> portalEngines;
  @Autowired private PortalEngineRegistry portalEngineRegistry;
  @Autowired private DataQualityEngine dataQualityEngine;
  @Autowired private PropertyFilterEngine propertyFilterEngine;
  @Autowired private MarketAnalyticsEngine marketAnalyticsEngine;
  @Autowired private DeduplicationEngine deduplicationEngine;
  @Autowired private SearchExecutionEngine searchExecutionEngine;
  @Autowired private IngatlanComHtmlImportService htmlImportService;
  @Autowired private DashboardPresentationService dashboardPresentationService;
  @Autowired private DedupPresentationService dedupPresentationService;
  @Autowired private IngatlanSearchService ingatlanSearchService;
  @Autowired private ListingStorage listingStorage;
  @Autowired private ConfigStorage configStorage;

  @Test
  @DisplayName("Minden 6 portálmotor automatikusan felderítve, determinisztikus sorrendben és közös normalizerrel")
  void testPortalEnginesAutoDiscoveredWithSharedNormalizer() {
    assertEquals(6, portalEngines.size());
    List<String> registeredIds =
        portalEngineRegistry.getAllDescriptors().stream().map(PortalDescriptor::id).toList();
    assertEquals(
        List.of("dh", "ingatlanbazar", "ingatlancom", "jofogas", "koltozzbe", "oc"),
        registeredIds);

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
    assertSame(listingNormalizer, RawListingNormalizer.getDefault());

    ListingNormalizer offlineInstance = new ListingNormalizer();
    assertNotNull(offlineInstance);
    assertSame(listingNormalizer, RawListingNormalizer.getDefault());
  }

  @Test
  @DisplayName("SearchExecutionEngine és presentation szolgáltatások a közös singleton beanekkel vannak bekötve")
  void testCoreAndPresentationSingletonWiring() {
    assertSame(portalEngineRegistry, searchExecutionEngine.getPortalRegistry());
    assertSame(listingStorage, searchExecutionEngine.getListingStorage());
    assertSame(configStorage, searchExecutionEngine.getConfigStorage());
    assertSame(propertyFilterEngine, searchExecutionEngine.getFilterEngine());
    assertSame(deduplicationEngine, searchExecutionEngine.getDedupEngine());
    assertSame(marketAnalyticsEngine, searchExecutionEngine.getAnalyticsEngine());
    assertSame(dataQualityEngine, searchExecutionEngine.getQualityEngine());
    assertNotNull(searchExecutionEngine.getScrapeExecutor());
    assertNotNull(searchExecutionEngine.getResultAggregator());

    assertNotNull(htmlImportService);
    assertNotNull(dashboardPresentationService);
    assertNotNull(dedupPresentationService);
    assertNotNull(ingatlanSearchService);
  }
}
