package ingatlanok.portal.koltozzbe;

import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalUrlBuilder;
import ingatlanok.portal.RawListingNormalizer;

public class KoltozzbePortalEngine extends AbstractAdapterPortalEngine {

  public static final int DEFAULT_MAX_PAGES = 20;

  private final KoltozzbeUrlBuilder koltozzbeUrlBuilder;
  private final int maxPages;

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
      KoltozzbeUrlBuilder urlBuilder, int maxPages, RawListingNormalizer normalizer) {
    this(
        urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder(),
        maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES,
        normalizer,
        true);
  }

  private KoltozzbePortalEngine(
      KoltozzbeUrlBuilder urlBuilder,
      int maxPages,
      RawListingNormalizer normalizer,
      boolean normalized) {
    super(
        urlBuilder,
        List.of(
            new KoltozzbeExtractionStrategy(
                urlBuilder, new KoltozzbeRawParser(), HttpFetcher::fetchDocument, maxPages)),
        normalizer);
    this.koltozzbeUrlBuilder = urlBuilder;
    this.maxPages = maxPages;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new KoltozzbePortalAdapter(builder, strategies);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    return scrape(criteria, documentFetcher, maxPages);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher, int customMaxPages) {
    return scrapeWithDynamicStrategies(
        criteria,
        List.of(
            new KoltozzbeExtractionStrategy(
                koltozzbeUrlBuilder, new KoltozzbeRawParser(), documentFetcher, customMaxPages)));
  }

  @Override
  public KoltozzbeUrlBuilder getUrlBuilder() {
    return koltozzbeUrlBuilder;
  }
}
