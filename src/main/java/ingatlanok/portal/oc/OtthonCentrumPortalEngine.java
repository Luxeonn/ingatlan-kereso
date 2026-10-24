package ingatlanok.portal.oc;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class OtthonCentrumPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(OtthonCentrumPortalEngine.class);

  private final OtthonCentrumUrlBuilder urlBuilder;
  private final OtthonCentrumParser parser;
  private final AdapterPortalEngine adapterEngine;

  public OtthonCentrumPortalEngine() {
    this(new OtthonCentrumUrlBuilder(), new OtthonCentrumParser());
  }

  public OtthonCentrumPortalEngine(OtthonCentrumUrlBuilder urlBuilder, OtthonCentrumParser parser) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder();
    this.parser = parser != null ? parser : new OtthonCentrumParser();
    this.adapterEngine = new AdapterPortalEngine(new OtthonCentrumPortalAdapter(this.urlBuilder,
        List.of(new OtthonCentrumHtmlStrategy(this.urlBuilder, new OtthonCentrumRawParser(),
            HttpFetcher::fetchDocument))));
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return adapterEngine.getDescriptor();
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return scrape(criteria, HttpFetcher::fetchDocument);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, java.util.function.Function<String, Document> documentFetcher) {
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new OtthonCentrumPortalAdapter(urlBuilder,
        List.of(new OtthonCentrumHtmlStrategy(urlBuilder, new OtthonCentrumRawParser(), documentFetcher))));
    return dynamicEngine.scrape(criteria);
  }

  public OtthonCentrumUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public OtthonCentrumParser getParser() {
    return parser;
  }
}
