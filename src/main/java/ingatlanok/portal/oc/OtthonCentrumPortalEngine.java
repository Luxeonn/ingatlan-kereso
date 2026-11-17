package ingatlanok.portal.oc;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OtthonCentrumPortalEngine extends AbstractAdapterPortalEngine {

  private final OtthonCentrumUrlBuilder ocUrlBuilder;

  public OtthonCentrumPortalEngine() {
    this(new OtthonCentrumUrlBuilder(), null);
  }

  @Autowired
  public OtthonCentrumPortalEngine(RawListingNormalizer normalizer) {
    this(
        new OtthonCentrumUrlBuilder(),
        Objects.requireNonNull(normalizer, "RawListingNormalizer nem lehet null"));
  }

  public OtthonCentrumPortalEngine(OtthonCentrumUrlBuilder urlBuilder) {
    this(urlBuilder, null);
  }

  public OtthonCentrumPortalEngine(
      OtthonCentrumUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    this(urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder(), normalizer, true);
  }

  private OtthonCentrumPortalEngine(
      OtthonCentrumUrlBuilder urlBuilder, RawListingNormalizer normalizer, boolean normalized) {
    super(
        urlBuilder,
        List.of(
            new OtthonCentrumHtmlStrategy(
                urlBuilder, new OtthonCentrumRawParser(), HttpFetcher::fetchDocument)),
        normalizer);
    this.ocUrlBuilder = urlBuilder;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new OtthonCentrumPortalAdapter(builder, strategies);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    return scrape(criteria, documentFetcher, OtthonCentrumHtmlStrategy.DEFAULT_MAX_PAGES);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher, int maxPages) {
    return scrape(criteria, documentFetcher, HttpFetcher::postJsonDocument, maxPages);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria,
      Function<String, Document> documentFetcher,
      BiFunction<String, String, Document> chunkDocumentFetcher,
      int maxPages) {
    return scrapeWithDynamicStrategies(
        criteria,
        List.of(
            new OtthonCentrumHtmlStrategy(
                ocUrlBuilder,
                new OtthonCentrumRawParser(),
                documentFetcher,
                chunkDocumentFetcher,
                maxPages)));
  }

  @Override
  public OtthonCentrumUrlBuilder getUrlBuilder() {
    return ocUrlBuilder;
  }
}
