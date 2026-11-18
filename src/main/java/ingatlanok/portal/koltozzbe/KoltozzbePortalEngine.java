package ingatlanok.portal.koltozzbe;

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
public class KoltozzbePortalEngine extends AbstractAdapterPortalEngine {

  public static final int DEFAULT_MAX_PAGES = 20;

  private final KoltozzbeUrlBuilder koltozzbeUrlBuilder;

  public KoltozzbePortalEngine(KoltozzbeUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    super(
        Objects.requireNonNull(urlBuilder, "KoltozzbeUrlBuilder nem lehet null"),
        List.of(
            new KoltozzbeExtractionStrategy(
                urlBuilder,
                new KoltozzbeRawParser(),
                HttpFetcher::fetchDocument,
                DEFAULT_MAX_PAGES)),
        Objects.requireNonNull(normalizer, "RawListingNormalizer nem lehet null"));
    this.koltozzbeUrlBuilder = urlBuilder;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new KoltozzbePortalAdapter(builder, strategies);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    return scrape(criteria, documentFetcher, DEFAULT_MAX_PAGES);
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
