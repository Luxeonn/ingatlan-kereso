package ingatlanok.portal.ingatlanbazar;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
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

  public IngatlanBazarPortalEngine() {
    this(new IngatlanBazarQueryBuilder(), null);
  }

  @Autowired
  public IngatlanBazarPortalEngine(RawListingNormalizer normalizer) {
    this(
        new IngatlanBazarQueryBuilder(),
        Objects.requireNonNull(normalizer, "RawListingNormalizer nem lehet null"));
  }

  public IngatlanBazarPortalEngine(IngatlanBazarQueryBuilder queryBuilder) {
    this(queryBuilder, null);
  }

  public IngatlanBazarPortalEngine(
      IngatlanBazarQueryBuilder queryBuilder, RawListingNormalizer normalizer) {
    this(queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder(), normalizer, true);
  }

  private IngatlanBazarPortalEngine(
      IngatlanBazarQueryBuilder queryBuilder, RawListingNormalizer normalizer, boolean normalized) {
    super(
        queryBuilder,
        List.of(
            new IngatlanBazarApiStrategy(
                queryBuilder, new IngatlanBazarRawParser(), HttpFetcher::fetchJson)),
        normalizer);
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
