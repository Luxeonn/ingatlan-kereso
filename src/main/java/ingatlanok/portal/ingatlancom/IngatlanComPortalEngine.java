package ingatlanok.portal.ingatlancom;

import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalUrlBuilder;
import ingatlanok.portal.RawListingNormalizer;

public class IngatlanComPortalEngine extends AbstractAdapterPortalEngine {

  private final IngatlanComUrlBuilder icUrlBuilder;

  public IngatlanComPortalEngine() {
    this(new IngatlanComUrlBuilder(), null);
  }

  public IngatlanComPortalEngine(IngatlanComUrlBuilder urlBuilder) {
    this(urlBuilder, null);
  }

  public IngatlanComPortalEngine(
      IngatlanComUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    this(urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder(), normalizer, true);
  }

  private IngatlanComPortalEngine(
      IngatlanComUrlBuilder urlBuilder, RawListingNormalizer normalizer, boolean normalized) {
    super(
        urlBuilder,
        List.of(
            new IngatlanComSidecarStrategy(urlBuilder),
            new IngatlanComHtmlStrategy(urlBuilder, new IngatlanComRawParser(), null)),
        normalizer);
    this.icUrlBuilder = urlBuilder;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new IngatlanComPortalAdapter(builder, strategies);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    return scrapeWithDynamicStrategies(
        criteria,
        List.of(
            new IngatlanComHtmlStrategy(
                icUrlBuilder, new IngatlanComRawParser(), documentFetcher)));
  }

  @Override
  public IngatlanComUrlBuilder getUrlBuilder() {
    return icUrlBuilder;
  }
}
