package ingatlanok.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.presentation.DashboardPresentationService;

@Configuration
public class CoreEngineConfig {

  @Bean
  public PortalEngineRegistry portalEngineRegistry(List<PortalEngine> engines) {
    return new PortalEngineRegistry(engines);
  }

  @Bean
  public DunaHousePortalEngine dunaHousePortalEngine() {
    return new DunaHousePortalEngine();
  }

  @Bean
  public OtthonCentrumPortalEngine otthonCentrumPortalEngine() {
    return new OtthonCentrumPortalEngine();
  }

  @Bean
  public JofogasPortalEngine jofogasPortalEngine() {
    return new JofogasPortalEngine();
  }

  @Bean
  public IngatlanBazarPortalEngine ingatlanBazarPortalEngine() {
    return new IngatlanBazarPortalEngine();
  }

  @Bean
  public KoltozzbePortalEngine koltozzbePortalEngine() {
    return new KoltozzbePortalEngine();
  }

  @Bean
  public IngatlanComPortalEngine ingatlanComPortalEngine() {
    return new IngatlanComPortalEngine();
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
