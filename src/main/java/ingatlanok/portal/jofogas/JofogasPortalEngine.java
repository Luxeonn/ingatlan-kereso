package ingatlanok.portal.jofogas;

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

public class JofogasPortalEngine extends AbstractAdapterPortalEngine {

  private final JofogasUrlBuilder jofogasUrlBuilder;

  public JofogasPortalEngine() {
    this(new JofogasUrlBuilder(), null);
  }

  public JofogasPortalEngine(JofogasUrlBuilder urlBuilder) {
    this(urlBuilder, null);
  }

  public JofogasPortalEngine(JofogasUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    this(urlBuilder != null ? urlBuilder : new JofogasUrlBuilder(), normalizer, true);
  }

  private JofogasPortalEngine(
      JofogasUrlBuilder urlBuilder, RawListingNormalizer normalizer, boolean normalized) {
    super(
        urlBuilder,
        List.of(
            new JofogasHtmlStrategy(
                urlBuilder, new JofogasRawParser(), HttpFetcher::fetchDocument)),
        normalizer);
    this.jofogasUrlBuilder = urlBuilder;
  }

  @Override
  protected PortalAdapter createAdapter(
      PortalUrlBuilder builder, List<ExtractionStrategy> strategies) {
    return new JofogasPortalAdapter(builder, strategies);
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    if (criteria == null) {
      return PortalScrapeResult.failure(
          "jofogas", "Jófogás Ingatlan", "Criteria is null", 0, 400, "https://ingatlan.jofogas.hu");
    }
    return super.scrape(criteria);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    return scrape(criteria, documentFetcher, JofogasHtmlStrategy.DEFAULT_MAX_PAGES);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, Function<String, Document> documentFetcher, int maxPages) {
    if (criteria == null) {
      return PortalScrapeResult.failure(
          "jofogas", "Jófogás Ingatlan", "Criteria is null", 0, 400, "https://ingatlan.jofogas.hu");
    }
    return scrapeWithDynamicStrategies(
        criteria,
        List.of(
            new JofogasHtmlStrategy(
                jofogasUrlBuilder, new JofogasRawParser(), documentFetcher, maxPages)));
  }

  @Override
  public JofogasUrlBuilder getUrlBuilder() {
    return jofogasUrlBuilder;
  }
}
