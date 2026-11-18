package ingatlanok.portal.oc;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractPaginatedHtmlStrategy;
import ingatlanok.portal.PaginationState;

public class OtthonCentrumHtmlStrategy extends AbstractPaginatedHtmlStrategy {

  public static final int DEFAULT_MAX_PAGES = AbstractPaginatedHtmlStrategy.DEFAULT_MAX_PAGES;

  private static final String CHUNK_FILTER_ATTR = "oc.chunkFilterJson";
  private static final String CHUNK_URL_PREFIX =
      OtthonCentrumUrlBuilder.BASE_URL + "/ingatlanok/kereses/chunk?page=";

  private final OtthonCentrumRawParser ocRawParser;
  private final BiFunction<String, String, Document> chunkDocumentFetcher;

  public OtthonCentrumHtmlStrategy() {
    this(new OtthonCentrumUrlBuilder(), new OtthonCentrumRawParser(), HttpFetcher::fetchDocument);
  }

  public OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this(urlBuilder, rawParser, documentFetcher, HttpFetcher::postJsonDocument, DEFAULT_MAX_PAGES);
  }

  public OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this(urlBuilder, rawParser, documentFetcher, HttpFetcher::postJsonDocument, maxPages);
  }

  public OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher,
      BiFunction<String, String, Document> chunkDocumentFetcher,
      int maxPages) {
    this(
        urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder(),
        rawParser != null ? rawParser : new OtthonCentrumRawParser(),
        documentFetcher,
        chunkDocumentFetcher,
        maxPages,
        true);
  }

  private OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher,
      BiFunction<String, String, Document> chunkDocumentFetcher,
      int maxPages,
      boolean normalized) {
    super(urlBuilder, rawParser, documentFetcher, maxPages, DEFAULT_MAX_PAGES);
    this.ocRawParser = rawParser;
    this.chunkDocumentFetcher =
        chunkDocumentFetcher != null ? chunkDocumentFetcher : HttpFetcher::postJsonDocument;
  }

  @Override
  protected String getPortalLogName() {
    return "Otthon Centrum";
  }

  @Override
  protected String getFallbackBaseUrl() {
    return OtthonCentrumUrlBuilder.BASE_URL;
  }

  @Override
  protected String buildPageUrl(
      SearchCriteria criteria, String searchUrl, int page, PaginationState state) {
    if (isChunkPaginationActive(page, state)) {
      return CHUNK_URL_PREFIX + page;
    }
    return super.buildPageUrl(criteria, searchUrl, page, state);
  }

  @Override
  protected Document fetchDocumentForPage(String pageUrl, int page, PaginationState state) {
    if (isChunkPaginationActive(page, state)) {
      return chunkDocumentFetcher.apply(pageUrl, state.getAttribute(CHUNK_FILTER_ATTR));
    }
    return super.fetchDocumentForPage(pageUrl, page, state);
  }

  @Override
  protected void onDocumentFetched(Document document, int page, PaginationState state) {
    if (page == 1) {
      String chunkFilterJson = ocRawParser.extractChunkFilterJson(document);
      if (chunkFilterJson != null && !chunkFilterJson.isBlank()) {
        state.putAttribute(CHUNK_FILTER_ATTR, chunkFilterJson);
      }
    }
  }

  @Override
  protected boolean hasPaginationWithoutNextPage(
      Document document, int page, PaginationState state) {
    String chunkFilterJson = state.getAttribute(CHUNK_FILTER_ATTR);
    if (chunkFilterJson != null && !chunkFilterJson.isBlank()) {
      return false;
    }
    return super.hasPaginationWithoutNextPage(document, page, state);
  }

  private boolean isChunkPaginationActive(int page, PaginationState state) {
    if (page < 2 || state == null) {
      return false;
    }
    String chunkFilterJson = state.getAttribute(CHUNK_FILTER_ATTR);
    return chunkFilterJson != null && !chunkFilterJson.isBlank();
  }
}
