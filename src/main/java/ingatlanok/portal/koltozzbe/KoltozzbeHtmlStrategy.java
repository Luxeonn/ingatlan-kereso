package ingatlanok.portal.koltozzbe;

import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.portal.AbstractPaginatedHtmlStrategy;
import ingatlanok.portal.PaginationState;
import ingatlanok.portal.PaginationStopReason;

public class KoltozzbeHtmlStrategy extends AbstractPaginatedHtmlStrategy {

  private static final int DEFAULT_MAX_PAGES = 3;

  private final KoltozzbeRawParser koltozzbeRawParser;

  public KoltozzbeHtmlStrategy() {
    this(new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), HttpFetcher::fetchDocument);
  }

  public KoltozzbeHtmlStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this(urlBuilder, rawParser, documentFetcher, DEFAULT_MAX_PAGES);
  }

  public KoltozzbeHtmlStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this(
        urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder(),
        rawParser != null ? rawParser : new KoltozzbeRawParser(),
        documentFetcher,
        maxPages,
        true);
  }

  private KoltozzbeHtmlStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages,
      boolean normalized) {
    super(urlBuilder, rawParser::parseHtmlListings, documentFetcher, maxPages, DEFAULT_MAX_PAGES);
    this.koltozzbeRawParser = rawParser;
  }

  @Override
  protected String getPortalLogName() {
    return "Költözzbe HTML";
  }

  @Override
  protected String getFallbackBaseUrl() {
    return KoltozzbeUrlBuilder.BASE_URL;
  }

  @Override
  protected boolean hasPaginationWithoutNextPage(
      Document document, int page, PaginationState state) {
    return !koltozzbeRawParser.hasNextPage(document, page);
  }

  @Override
  protected PaginationStopReason resolveNoMorePagesReason() {
    return PaginationStopReason.NO_NEXT_PAGE;
  }
}
