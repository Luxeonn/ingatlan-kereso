package ingatlanok.portal;

import java.util.List;
import java.util.Objects;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;

/**
 * Shared base class for portal engines delegating execution to {@link AdapterPortalEngine} without
 * generic type parameters.
 */
public abstract class AbstractAdapterPortalEngine implements PortalEngine {

  private final PortalUrlBuilder urlBuilder;
  private final AdapterPortalEngine adapterEngine;

  protected AbstractAdapterPortalEngine(
      PortalUrlBuilder urlBuilder,
      List<ExtractionStrategy> defaultStrategies,
      RawListingNormalizer normalizer) {
    this.urlBuilder = Objects.requireNonNull(urlBuilder, "PortalUrlBuilder cannot be null");
    this.adapterEngine =
        new AdapterPortalEngine(createAdapter(this.urlBuilder, defaultStrategies), normalizer);
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return adapterEngine.getDescriptor();
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return adapterEngine.scrape(criteria);
  }

  protected PortalScrapeResult scrapeWithDynamicStrategies(
      SearchCriteria criteria, List<ExtractionStrategy> strategies) {
    AdapterPortalEngine dynamicEngine =
        new AdapterPortalEngine(
            createAdapter(urlBuilder, strategies), adapterEngine.getNormalizer());
    return dynamicEngine.scrape(criteria);
  }

  protected abstract PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies);

  public PortalUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public RawListingNormalizer getNormalizer() {
    return adapterEngine.getNormalizer();
  }
}
