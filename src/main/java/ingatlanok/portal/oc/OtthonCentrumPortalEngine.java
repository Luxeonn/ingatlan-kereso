package ingatlanok.portal.oc;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.RawListingNormalizer;

public class OtthonCentrumPortalEngine implements PortalEngine {

  private final OtthonCentrumUrlBuilder urlBuilder;
  private final AdapterPortalEngine adapterEngine;

  public OtthonCentrumPortalEngine() {
    this(new OtthonCentrumUrlBuilder(), null);
  }

  public OtthonCentrumPortalEngine(OtthonCentrumUrlBuilder urlBuilder) {
    this(urlBuilder, null);
  }

  public OtthonCentrumPortalEngine(OtthonCentrumUrlBuilder urlBuilder, RawListingNormalizer normalizer) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder();
    this.adapterEngine = new AdapterPortalEngine(new OtthonCentrumPortalAdapter(this.urlBuilder,
        List.of(new OtthonCentrumHtmlStrategy(this.urlBuilder, new OtthonCentrumRawParser(),
            HttpFetcher::fetchDocument))),
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
    return scrape(criteria, documentFetcher, 2);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria,
      java.util.function.Function<String, Document> documentFetcher,
      int maxPages) {
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new OtthonCentrumPortalAdapter(urlBuilder,
        List.of(new OtthonCentrumHtmlStrategy(urlBuilder, new OtthonCentrumRawParser(), documentFetcher, maxPages))),
        adapterEngine.getNormalizer());
    return dynamicEngine.scrape(criteria);
  }

  public OtthonCentrumUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public RawListingNormalizer getNormalizer() {
    return adapterEngine.getNormalizer();
  }
}
