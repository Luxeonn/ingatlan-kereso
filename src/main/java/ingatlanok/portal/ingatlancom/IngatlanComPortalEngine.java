package ingatlanok.portal.ingatlancom;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractAdapterPortalEngine;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalUrlBuilder;
import ingatlanok.portal.RawListingNormalizer;

@Component
public class IngatlanComPortalEngine extends AbstractAdapterPortalEngine {

  private final IngatlanComUrlBuilder icUrlBuilder;

  public IngatlanComPortalEngine(
      IngatlanComUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    super(
        Objects.requireNonNull(urlBuilder, "IngatlanComUrlBuilder nem lehet null"),
        List.of(
            new IngatlanComSidecarStrategy(urlBuilder),
            new IngatlanComHtmlStrategy(urlBuilder, new IngatlanComRawParser(), null)),
        Objects.requireNonNull(normalizer, "RawListingNormalizer nem lehet null"));
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
