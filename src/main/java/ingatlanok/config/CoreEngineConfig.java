package ingatlanok.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.presentation.DashboardPresentationService;

/** Spring IoC konfiguráció a core motorokhoz és service rétegekhez. */
@Configuration
public class CoreEngineConfig {

  public CoreEngineConfig(ListingStorage listingStorage) {
    if (listingStorage != null) {
      ConfigStorage cs = (listingStorage instanceof ConfigStorage c) ? c : null;
      IngatlanRepository.getInstance().setStorage(listingStorage, cs);
    }
  }

  @Bean
  public PortalEngineRegistry portalEngineRegistry(List<PortalEngine> engines) {
    PortalEngineRegistry registry = PortalEngineRegistry.getInstance();
    if (engines != null) {
      for (PortalEngine engine : engines) {
        registry.register(engine);
      }
    }
    return registry;
  }

  @Bean
  public PropertyFilterEngine propertyFilterEngine() {
    return new PropertyFilterEngine();
  }

  @Bean
  public MarketAnalyticsEngine marketAnalyticsEngine() {
    return new MarketAnalyticsEngine();
  }

  @Bean
  public DeduplicationEngine deduplicationEngine(ListingStorage listingStorage) {
    return new DeduplicationEngine(listingStorage);
  }

  @Bean
  public SearchExecutionEngine searchExecutionEngine(
      PortalEngineRegistry portalRegistry,
      ListingStorage listingStorage,
      PropertyFilterEngine filterEngine,
      DeduplicationEngine dedupEngine,
      MarketAnalyticsEngine analyticsEngine) {
    ConfigStorage configStorage = (listingStorage instanceof ConfigStorage cs) ? cs : null;
    return new SearchExecutionEngine(
        portalRegistry, listingStorage, configStorage, filterEngine, dedupEngine, analyticsEngine);
  }

  @Bean
  public DashboardPresentationService dashboardPresentationService(
      SearchExecutionEngine searchEngine, MarketAnalyticsEngine analyticsEngine) {
    return new DashboardPresentationService(searchEngine, analyticsEngine);
  }
}
