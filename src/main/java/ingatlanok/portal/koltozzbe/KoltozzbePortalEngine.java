package ingatlanok.portal.koltozzbe;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.RawListingNormalizer;

public class KoltozzbePortalEngine implements PortalEngine {

  public static final int DEFAULT_MAX_PAGES = 5;

  private final KoltozzbeUrlBuilder urlBuilder;
  private final int maxPages;
  private final AdapterPortalEngine adapterEngine;

  public KoltozzbePortalEngine() {
    this(new KoltozzbeUrlBuilder(), DEFAULT_MAX_PAGES, null);
  }

  public KoltozzbePortalEngine(KoltozzbeUrlBuilder urlBuilder) {
    this(urlBuilder, DEFAULT_MAX_PAGES, null);
  }

  public KoltozzbePortalEngine(KoltozzbeUrlBuilder urlBuilder, int maxPages) {
    this(urlBuilder, maxPages, null);
  }

  public KoltozzbePortalEngine(
      KoltozzbeUrlBuilder urlBuilder,
      int maxPages,
      RawListingNormalizer normalizer) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
    this.adapterEngine = new AdapterPortalEngine(new KoltozzbePortalAdapter(this.urlBuilder,
        List.of(
            new KoltozzbeExtractionStrategy(this.urlBuilder, new KoltozzbeRawParser(),
                HttpFetcher::fetchDocument, this.maxPages)
        )),
        normalizer);
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
      SearchCriteria criteria, java.util.function.Function<String, Document> documentFetcher) {
    return scrape(criteria, documentFetcher, maxPages);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria,
      java.util.function.Function<String, Document> documentFetcher,
      int customMaxPages) {
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new KoltozzbePortalAdapter(urlBuilder,
        List.of(
            new KoltozzbeExtractionStrategy(
                urlBuilder, new KoltozzbeRawParser(), documentFetcher, customMaxPages)
        )),
        adapterEngine.getNormalizer());
    return dynamicEngine.scrape(criteria);
  }

  public KoltozzbeUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public RawListingNormalizer getNormalizer() {
    return adapterEngine.getNormalizer();
  }
}
