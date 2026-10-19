package ingatlanok.scraper;

import org.jsoup.nodes.Element;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;

public class KoltozzbeScraper extends BaseScraper implements IngatlanScraper {

  private final KoltozzbePortalEngine engine;

  public KoltozzbeScraper() {
    this.engine =
        (KoltozzbePortalEngine)
            PortalEngineRegistry.getInstance()
                .getEngine("koltozzbe")
                .orElseGet(KoltozzbePortalEngine::new);
  }

  public KoltozzbeScraper(KoltozzbePortalEngine engine) {
    this.engine = engine != null ? engine : new KoltozzbePortalEngine();
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
