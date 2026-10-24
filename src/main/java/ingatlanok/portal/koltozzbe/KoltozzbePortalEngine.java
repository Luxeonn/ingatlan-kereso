package ingatlanok.portal.koltozzbe;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class KoltozzbePortalEngine implements PortalEngine {

  public static final int DEFAULT_MAX_PAGES = 5;

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbePortalEngine.class);

  private final KoltozzbeUrlBuilder urlBuilder;
  private final KoltozzbeParser parser;
  private final int maxPages;
  private final AdapterPortalEngine adapterEngine;

  public KoltozzbePortalEngine() {
    this(new KoltozzbeUrlBuilder(), new KoltozzbeParser(), DEFAULT_MAX_PAGES);
  }

  public KoltozzbePortalEngine(KoltozzbeUrlBuilder urlBuilder, KoltozzbeParser parser) {
    this(urlBuilder, parser, DEFAULT_MAX_PAGES);
  }

  public KoltozzbePortalEngine(
      KoltozzbeUrlBuilder urlBuilder, KoltozzbeParser parser, int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.parser = parser != null ? parser : new KoltozzbeParser();
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
    this.adapterEngine = new AdapterPortalEngine(new KoltozzbePortalAdapter(this.urlBuilder,
        List.of(
            new KoltozzbeExtractionStrategy(this.urlBuilder, new KoltozzbeRawParser(), this.parser,
                HttpFetcher::fetchDocument, this.maxPages)
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
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new KoltozzbePortalAdapter(urlBuilder,
        List.of(
            new KoltozzbeExtractionStrategy(urlBuilder, new KoltozzbeRawParser(), parser, documentFetcher, maxPages)
        )));
    return dynamicEngine.scrape(criteria);
  }

  public KoltozzbeUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public KoltozzbeParser getParser() {
    return parser;
  }

  public int getMaxPages() {
    return maxPages;
  }
}
