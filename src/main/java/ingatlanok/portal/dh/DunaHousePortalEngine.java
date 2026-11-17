package ingatlanok.portal.dh;

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

public class DunaHousePortalEngine extends AbstractAdapterPortalEngine {

  private final DunaHouseUrlBuilder dhUrlBuilder;

  public DunaHousePortalEngine() {
    this(new DunaHouseUrlBuilder(), null);
  }

  public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder) {
    this(urlBuilder, null);
  }

  public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    this(urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder(), normalizer, true);
  }

  private DunaHousePortalEngine(
      DunaHouseUrlBuilder urlBuilder, RawListingNormalizer normalizer, boolean normalized) {
    super(
        urlBuilder,
        List.of(
            new DunaHouseHtmlStrategy(
                urlBuilder, new DunaHouseRawParser(), HttpFetcher::fetchDocument)),
        normalizer);
    this.dhUrlBuilder = urlBuilder;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new DunaHousePortalAdapter(builder, strategies);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    return scrape(criteria, documentFetcher, DunaHouseHtmlStrategy.DEFAULT_MAX_PAGES);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher, int maxPages) {
    return scrapeWithDynamicStrategies(
        criteria,
        List.of(
            new DunaHouseHtmlStrategy(
                dhUrlBuilder, new DunaHouseRawParser(), documentFetcher, maxPages)));
  }

  @Override
  public DunaHouseUrlBuilder getUrlBuilder() {
    return dhUrlBuilder;
  }
}
