package ingatlanok.portal.dh;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalUrlBuilder;
import ingatlanok.portal.RawListingNormalizer;

@Component
public class DunaHousePortalEngine extends AbstractAdapterPortalEngine {

  private final DunaHouseUrlBuilder dhUrlBuilder;

  public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    super(
        Objects.requireNonNull(urlBuilder, "DunaHouseUrlBuilder nem lehet null"),
        List.of(
            new DunaHouseHtmlStrategy(
                urlBuilder, new DunaHouseRawParser(), HttpFetcher::fetchDocument)),
        Objects.requireNonNull(normalizer, "RawListingNormalizer nem lehet null"));
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
