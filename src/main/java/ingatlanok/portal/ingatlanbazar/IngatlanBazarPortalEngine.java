package ingatlanok.portal.ingatlanbazar;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalUrlBuilder;
import ingatlanok.portal.RawListingNormalizer;

@Component
public class IngatlanBazarPortalEngine extends AbstractAdapterPortalEngine {

  private final IngatlanBazarQueryBuilder queryBuilder;

  public IngatlanBazarPortalEngine(
      IngatlanBazarQueryBuilder queryBuilder, RawListingNormalizer normalizer) {
    super(
        Objects.requireNonNull(queryBuilder, "IngatlanBazarQueryBuilder nem lehet null"),
        List.of(
            new IngatlanBazarApiStrategy(
                queryBuilder, new IngatlanBazarRawParser(), HttpFetcher::fetchJson)),
        Objects.requireNonNull(normalizer, "RawListingNormalizer nem lehet null"));
    this.queryBuilder = queryBuilder;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new IngatlanBazarPortalAdapter(builder, strategies);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, String> jsonFetcher) {
    return scrape(criteria, jsonFetcher, IngatlanBazarApiStrategy.DEFAULT_MAX_PAGES);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, String> jsonFetcher, int maxPages) {
    return scrapeWithDynamicStrategies(
        criteria,
        List.of(
            new IngatlanBazarApiStrategy(
                queryBuilder, new IngatlanBazarRawParser(), jsonFetcher, maxPages)));
  }

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }
}
