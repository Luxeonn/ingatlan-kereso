package ingatlanok.portal.jofogas;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class JofogasPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(JofogasPortalEngine.class);

  private final JofogasUrlBuilder urlBuilder;
  private final JofogasParser parser;
  private final AdapterPortalEngine adapterEngine;

  public JofogasPortalEngine() {
    this(new JofogasUrlBuilder(), new JofogasParser());
  }

  public JofogasPortalEngine(JofogasUrlBuilder urlBuilder, JofogasParser parser) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new JofogasUrlBuilder();
    this.parser = parser != null ? parser : new JofogasParser();
    this.adapterEngine = new AdapterPortalEngine(new JofogasPortalAdapter(this.urlBuilder,
        List.of(new JofogasHtmlStrategy(this.urlBuilder, new JofogasRawParser(), HttpFetcher::fetchDocument))));
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return adapterEngine.getDescriptor();
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return scrape(criteria, HttpFetcher::fetchDocument);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, java.util.function.Function<String, Document> documentFetcher) {
    if (criteria == null) {
      return PortalScrapeResult.failure("jofogas", "Jófogás Ingatlan", "Criteria is null", 0, 400, "https://ingatlan.jofogas.hu");
    }
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new JofogasPortalAdapter(urlBuilder,
        List.of(new JofogasHtmlStrategy(urlBuilder, new JofogasRawParser(), documentFetcher))));
    return dynamicEngine.scrape(criteria);
  }

  public JofogasUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public JofogasParser getParser() {
    return parser;
  }
}
