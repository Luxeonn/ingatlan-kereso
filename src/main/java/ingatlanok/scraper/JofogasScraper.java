package ingatlanok.scraper;

import org.jsoup.nodes.Element;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.jofogas.JofogasPortalEngine;

public class JofogasScraper extends BaseScraper implements IngatlanScraper {

  private final JofogasPortalEngine engine;

  public JofogasScraper() {
    this.engine =
        (JofogasPortalEngine)
            PortalEngineRegistry.getInstance()
                .getEngine("jofogas")
                .orElseGet(JofogasPortalEngine::new);
  }

  public JofogasScraper(JofogasPortalEngine engine) {
    this.engine = engine != null ? engine : new JofogasPortalEngine();
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
    return engine.getUrlBuilder().buildSearchUrl(criteria);
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return engine.scrape(criteria, this::getDocument);
  }
}
