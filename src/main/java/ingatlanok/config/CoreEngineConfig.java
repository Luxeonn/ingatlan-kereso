package ingatlanok.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ingatlanok.core.DataQualityEngine;
import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
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

@Configuration
public class CoreEngineConfig {

  @Bean
  public ListingNormalizer listingNormalizer() {
    ListingNormalizer normalizer = new ListingNormalizer();
    RawListingNormalizer.setDefault(normalizer);
    return normalizer;
  }

  @Bean
  public DunaHousePortalEngine dunaHousePortalEngine(ListingNormalizer listingNormalizer) {
    return new DunaHousePortalEngine(new DunaHouseUrlBuilder(), listingNormalizer);
  }

  @Bean
  public OtthonCentrumPortalEngine otthonCentrumPortalEngine(ListingNormalizer listingNormalizer) {
    return new OtthonCentrumPortalEngine(new OtthonCentrumUrlBuilder(), listingNormalizer);
  }

  @Bean
  public JofogasPortalEngine jofogasPortalEngine(ListingNormalizer listingNormalizer) {
    return new JofogasPortalEngine(new JofogasUrlBuilder(), listingNormalizer);
  }

  @Bean
  public IngatlanBazarPortalEngine ingatlanBazarPortalEngine(ListingNormalizer listingNormalizer) {
    return new IngatlanBazarPortalEngine(new IngatlanBazarQueryBuilder(), listingNormalizer);
  }

  @Bean
  public KoltozzbePortalEngine koltozzbePortalEngine(ListingNormalizer listingNormalizer) {
    return new KoltozzbePortalEngine(
        new KoltozzbeUrlBuilder(), KoltozzbePortalEngine.DEFAULT_MAX_PAGES, listingNormalizer);
  }

  @Bean
  public IngatlanComPortalEngine ingatlanComPortalEngine(ListingNormalizer listingNormalizer) {
    return new IngatlanComPortalEngine(new IngatlanComUrlBuilder(), listingNormalizer);
  }

  @Bean
  public PortalEngineRegistry portalEngineRegistry(List<PortalEngine> engines) {
    return new PortalEngineRegistry(engines);
  }

  @Bean
  public DataQualityEngine dataQualityEngine() {
    return new DataQualityEngine();
  }

  @Bean
  public PropertyFilterEngine propertyFilterEngine() {
    return new PropertyFilterEngine();
  }

  @Bean
  public MarketAnalyticsEngine marketAnalyticsEngine(DataQualityEngine qualityEngine) {
    return new MarketAnalyticsEngine(qualityEngine);
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
      MarketAnalyticsEngine analyticsEngine,
      DataQualityEngine qualityEngine) {
    ConfigStorage configStorage = (listingStorage instanceof ConfigStorage cs) ? cs : null;
    return SearchExecutionEngine.builder()
        .portalRegistry(portalRegistry)
        .listingStorage(listingStorage)
        .configStorage(configStorage)
        .filterEngine(filterEngine)
        .dedupEngine(dedupEngine)
        .analyticsEngine(analyticsEngine)
        .qualityEngine(qualityEngine)
        .build();
  }

  @Bean
  public DashboardPresentationService dashboardPresentationService(
      SearchExecutionEngine searchEngine, MarketAnalyticsEngine analyticsEngine) {
    return new DashboardPresentationService(searchEngine, analyticsEngine);
  }
}
