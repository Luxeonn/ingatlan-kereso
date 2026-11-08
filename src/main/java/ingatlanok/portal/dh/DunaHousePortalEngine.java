package ingatlanok.portal.dh;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AdapterPortalEngine;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;

public class DunaHousePortalEngine implements PortalEngine {

  private final DunaHouseUrlBuilder urlBuilder;
  private final AdapterPortalEngine adapterEngine;

  public DunaHousePortalEngine() {
    this(new DunaHouseUrlBuilder());
  }

  public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder();
    this.adapterEngine = new AdapterPortalEngine(new DunaHousePortalAdapter(this.urlBuilder,
        List.of(new DunaHouseHtmlStrategy(this.urlBuilder, new DunaHouseRawParser(), HttpFetcher::fetchDocument))));
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
    AdapterPortalEngine dynamicEngine = new AdapterPortalEngine(new DunaHousePortalAdapter(urlBuilder,
        List.of(new DunaHouseHtmlStrategy(urlBuilder, new DunaHouseRawParser(), documentFetcher, maxPages))));
    return dynamicEngine.scrape(criteria);
  }

  public DunaHouseUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }
}
