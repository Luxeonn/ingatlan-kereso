package ingatlanok.portal.dh;

import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.portal.AbstractPaginatedHtmlStrategy;

public class DunaHouseHtmlStrategy extends AbstractPaginatedHtmlStrategy {

  public static final int DEFAULT_MAX_PAGES = AbstractPaginatedHtmlStrategy.DEFAULT_MAX_PAGES;

  public DunaHouseHtmlStrategy() {
    this(new DunaHouseUrlBuilder(), new DunaHouseRawParser(), HttpFetcher::fetchDocument);
  }

  public DunaHouseHtmlStrategy(
      DunaHouseUrlBuilder urlBuilder,
      DunaHouseRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this(urlBuilder, rawParser, documentFetcher, DEFAULT_MAX_PAGES);
  }

  public DunaHouseHtmlStrategy(
      DunaHouseUrlBuilder urlBuilder,
      DunaHouseRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    super(
        urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder(),
        rawParser != null ? rawParser : new DunaHouseRawParser(),
        documentFetcher,
        maxPages,
        DEFAULT_MAX_PAGES);
  }

  @Override
  protected String getPortalLogName() {
    return "Duna House";
  }

  @Override
  protected String getFallbackBaseUrl() {
    return DunaHouseUrlBuilder.BASE_URL;
  }
}
