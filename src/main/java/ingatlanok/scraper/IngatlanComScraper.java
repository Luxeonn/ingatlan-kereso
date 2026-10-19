package ingatlanok.scraper;

import org.jsoup.nodes.Element;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;

public class IngatlanComScraper extends BaseScraper implements IngatlanScraper {

  private final IngatlanComPortalEngine engine;

  public IngatlanComScraper() {
    this.engine =
        (IngatlanComPortalEngine)
            PortalEngineRegistry.getInstance()
                .getEngine("ingatlancom")
                .orElseGet(IngatlanComPortalEngine::new);
  }

  public IngatlanComScraper(IngatlanComPortalEngine engine) {
    this.engine = engine != null ? engine : new IngatlanComPortalEngine();
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
    return 20;
  }

  public String buildSearchUrl(SearchCriteria criteria) {
    return engine.getUrlBuilder().buildSearchUrl(criteria);
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return engine.scrape(criteria, this::getDocument);
  }
}
