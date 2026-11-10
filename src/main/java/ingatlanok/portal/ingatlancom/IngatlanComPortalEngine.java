package ingatlanok.portal.ingatlancom;

import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.RawListingNormalizer;

public class IngatlanComPortalEngine implements PortalEngine {

  private final IngatlanComUrlBuilder urlBuilder;
  private final AdapterPortalEngine adapterEngine;

  public IngatlanComPortalEngine() {
    this(new IngatlanComUrlBuilder(), null);
  }

  public IngatlanComPortalEngine(IngatlanComUrlBuilder urlBuilder) {
    this(urlBuilder, null);
  }

  public IngatlanComPortalEngine(IngatlanComUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
    this.adapterEngine =
        new AdapterPortalEngine(
            new IngatlanComPortalAdapter(
                this.urlBuilder,
                List.of(
                    new IngatlanComSidecarStrategy(this.urlBuilder),
                    new IngatlanComHtmlStrategy(
                        this.urlBuilder,
                        new IngatlanComRawParser(),
                        null))),
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
      SearchCriteria criteria, Function<String, Document> documentFetcher) {
    AdapterPortalEngine dynamicEngine =
        new AdapterPortalEngine(
            new IngatlanComPortalAdapter(
                urlBuilder,
                List.of(
                    new IngatlanComHtmlStrategy(
                        urlBuilder, new IngatlanComRawParser(), documentFetcher))),
            adapterEngine.getNormalizer());
    return dynamicEngine.scrape(criteria);
  }

  public IngatlanComUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public RawListingNormalizer getNormalizer() {
    return adapterEngine.getNormalizer();
  }
}
