package ingatlanok.portal.ingatlanbazar;

import java.util.List;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class IngatlanBazarPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarPortalEngine.class);

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final IngatlanBazarJsonParser jsonParser;
  private final AdapterPortalEngine adapterEngine;

  public IngatlanBazarPortalEngine() {
    this(new IngatlanBazarQueryBuilder(), new IngatlanBazarJsonParser());
  }

  public IngatlanBazarPortalEngine(
      IngatlanBazarQueryBuilder queryBuilder, IngatlanBazarJsonParser jsonParser) {
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.jsonParser = jsonParser != null ? jsonParser : new IngatlanBazarJsonParser();
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
    return scrape(criteria, HttpFetcher::fetchJson);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, java.util.function.Function<String, String> jsonFetcher) {
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new IngatlanBazarPortalAdapter(queryBuilder,
        List.of(new IngatlanBazarApiStrategy(queryBuilder, new IngatlanBazarRawParser(), jsonFetcher))));
    return dynamicEngine.scrape(criteria);
  }

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  public IngatlanBazarJsonParser getJsonParser() {
    return jsonParser;
  }
}
