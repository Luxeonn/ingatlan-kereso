package ingatlanok.portal.ingatlancom;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class IngatlanComPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComPortalEngine.class);

  private final IngatlanComUrlBuilder urlBuilder;
  private final IngatlanComParser parser;
  private final AdapterPortalEngine adapterEngine;

  public IngatlanComPortalEngine() {
    this(new IngatlanComUrlBuilder(), new IngatlanComParser());
  }

  public IngatlanComPortalEngine(IngatlanComUrlBuilder urlBuilder, IngatlanComParser parser) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
    this.parser = parser != null ? parser : new IngatlanComParser();
    this.adapterEngine = new AdapterPortalEngine(new IngatlanComPortalAdapter(this.urlBuilder,
        List.of(
            new IngatlanComSidecarStrategy(this.urlBuilder),
            new IngatlanComHtmlStrategy(this.urlBuilder, new IngatlanComRawParser(), this.parser,
                HttpFetcher::fetchDocument)
        )));
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
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new IngatlanComPortalAdapter(urlBuilder,
        List.of(
            new IngatlanComSidecarStrategy(urlBuilder),
            new IngatlanComHtmlStrategy(urlBuilder, new IngatlanComRawParser(), parser, documentFetcher)
        )));
    return dynamicEngine.scrape(criteria);
  }

  public IngatlanComUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public IngatlanComParser getParser() {
    return parser;
  }
}
