package ingatlanok.scraper;

import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Element;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;

public class IngatlanBazarScraper extends BaseScraper implements IngatlanScraper {

  public static final String BASE_URL = IngatlanBazarQueryBuilder.BASE_URL;
  public static final String API_SEARCH_URL = IngatlanBazarQueryBuilder.API_SEARCH_URL;

  private final IngatlanBazarPortalEngine engine;

  public IngatlanBazarScraper() {
    this.engine =
        (IngatlanBazarPortalEngine)
            PortalEngineRegistry.getInstance()
                .getEngine("ingatlanbazar")
                .orElseGet(IngatlanBazarPortalEngine::new);
  }

  public IngatlanBazarScraper(IngatlanBazarPortalEngine engine) {
    this.engine = engine != null ? engine : new IngatlanBazarPortalEngine();
  }

  @Override
  public String getPortalId() {
    return engine.getId();
  }

  @Override
  public String getName() {
    return engine.getName();
  }

  @Override
  public void init() {}

  @Override
  public int getCardNumber(Element resultPageSize) {
    return 25;
  }

  public String buildSearchUrl(SearchCriteria criteria) {
    return engine.getQueryBuilder().buildSearchUrl(criteria);
  }

  public String buildApiSearchUrl(SearchCriteria criteria, int page) {
    return engine.getQueryBuilder().buildApiSearchUrl(criteria, page);
  }

  public List<IngatlanItem> parseItemsFromJson(
      String json, SearchCriteria criteria, Set<String> processedUrls, String now) {
    return engine.getJsonParser().parseItemsFromJson(json, criteria, processedUrls, now);
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return engine.scrape(criteria, this::getJson);
  }
}
