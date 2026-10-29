package ingatlanok.portal.dh;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class DunaHousePortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(DunaHousePortalEngine.class);

  private final DunaHouseUrlBuilder urlBuilder;
  private final DunaHouseParser parser;
  private final AdapterPortalEngine adapterEngine;

  public DunaHousePortalEngine() {
    this(new DunaHouseUrlBuilder(), new DunaHouseParser());
  }

  public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder, DunaHouseParser parser) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder();
    this.parser = parser != null ? parser : new DunaHouseParser();
    this.adapterEngine = new AdapterPortalEngine(new DunaHousePortalAdapter(this.urlBuilder,
        List.of(new DunaHouseHtmlStrategy(this.urlBuilder, new DunaHouseRawParser(), HttpFetcher::fetchDocument))));
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
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new DunaHousePortalAdapter(urlBuilder,
        List.of(new DunaHouseHtmlStrategy(urlBuilder, new DunaHouseRawParser(), documentFetcher))));
    return dynamicEngine.scrape(criteria);
  }

  public DunaHouseUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public DunaHouseParser getParser() {
    return parser;
  }
}
