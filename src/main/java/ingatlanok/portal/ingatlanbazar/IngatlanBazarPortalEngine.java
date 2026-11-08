package ingatlanok.portal.ingatlanbazar;

import java.util.List;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class IngatlanBazarPortalEngine implements PortalEngine {

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final AdapterPortalEngine adapterEngine;

  public IngatlanBazarPortalEngine() {
    this(new IngatlanBazarQueryBuilder());
  }

  public IngatlanBazarPortalEngine(IngatlanBazarQueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.adapterEngine = new AdapterPortalEngine(new IngatlanBazarPortalAdapter(this.queryBuilder,
        List.of(new IngatlanBazarApiStrategy(this.queryBuilder, new IngatlanBazarRawParser(),
            HttpFetcher::fetchJson))));
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return adapterEngine.getDescriptor();
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return adapterEngine.scrape(criteria);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, java.util.function.Function<String, String> jsonFetcher) {
    return scrape(criteria, jsonFetcher, 2);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria,
      java.util.function.Function<String, String> jsonFetcher,
      int maxPages) {
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new IngatlanBazarPortalAdapter(queryBuilder,
        List.of(new IngatlanBazarApiStrategy(queryBuilder, new IngatlanBazarRawParser(), jsonFetcher, maxPages))));
    return dynamicEngine.scrape(criteria);
  }

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }
}
