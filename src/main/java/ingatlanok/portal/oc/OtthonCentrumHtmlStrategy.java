package ingatlanok.portal.oc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalSupport;

public class OtthonCentrumHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(OtthonCentrumHtmlStrategy.class);
  private static final int DEFAULT_MAX_PAGES = 2;

  private final OtthonCentrumUrlBuilder urlBuilder;
  private final OtthonCentrumRawParser rawParser;
  private final Function<String, Document> documentFetcher;
  private final int maxPages;

  public OtthonCentrumHtmlStrategy() {
    this(new OtthonCentrumUrlBuilder(), new OtthonCentrumRawParser(), HttpFetcher::fetchDocument);
  }

  public OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this(urlBuilder, rawParser, documentFetcher, DEFAULT_MAX_PAGES);
  }

  public OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new OtthonCentrumRawParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    LOGGER.info("Otthon Centrum HTML stratégia indítása: {}", searchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    Set<String> processedNativeIds = new HashSet<>();
    Set<String> visitedPageUrls = new HashSet<>();
    int pagesProcessed = 0;
    String stopReason = "MAX_PAGES_REACHED";

    for (int i = 1; i <= maxPages; i++) {
      String pageUrl = urlBuilder.buildPageUrl(searchUrl, i);
      if (pageUrl == null || !visitedPageUrls.add(pageUrl)) {
        stopReason = "DUPLICATE_PAGE_URL";
        break;
      }

      PageFetchOutcome fetchOutcome = fetchPageDocument(pageUrl, i);
      if (fetchOutcome.botProtected()) {
        if (allListings.isEmpty()) {
          return RawExtractionResult.botProtected(method(), searchUrl, "Bot-védelem detektálva");
        }
        stopReason = "INTERMEDIATE_PAGE_BOT_PROTECTED";
        break;
      }
      if (fetchOutcome.document() == null) {
        if (i == 1 && allListings.isEmpty()) {
          return RawExtractionResult.failure(
              method(), searchUrl, fetchOutcome.errorMessage(), 500);
        }
        stopReason = fetchOutcome.stopReason();
        break;
      }

      pagesProcessed++;
      String pageStopReason = processPageDocument(
          fetchOutcome.document(), i, processedUrls, processedNativeIds, allListings);
      if (pageStopReason != null) {
        stopReason = pageStopReason;
        break;
      }
    }

    LOGGER.info("Otthon Centrum lapozás befejezve: feldolgozott oldalak={}, találatok={}, leállás oka={}",
        pagesProcessed, allListings.size(), stopReason);

    if (allListings.isEmpty()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }
    String details = String.format("pages=%d, stopReason=%s", pagesProcessed, stopReason);
    return RawExtractionResult.success(allListings, method(), details);
  }

  private PageFetchOutcome fetchPageDocument(String pageUrl, int pageIndex) {
    try {
      Document document = documentFetcher.apply(pageUrl);
      if (document == null) {
        LOGGER.warn("Nem sikerült letölteni az Otthon Centrum {}. oldalt: {}", pageIndex, pageUrl);
        return new PageFetchOutcome(
            null, false, "INTERMEDIATE_PAGE_FETCH_FAILED", "Nem sikerült letölteni az oldalt");
      }
      if (PortalSupport.isCommonBotChallenge(document)) {
        LOGGER.warn("Otthon Centrum: Bot-védelem detektálva");
        return new PageFetchOutcome(null, true, "BOT_PROTECTED", "Bot-védelem detektálva");
      }
      return new PageFetchOutcome(document, false, null, null);
    } catch (Exception e) {
      LOGGER.error("Hiba az Otthon Centrum {}. oldal lekérésekor: {}", pageIndex, e.getMessage(), e);
      return new PageFetchOutcome(null, false, "INTERMEDIATE_PAGE_EXCEPTION", e.getMessage());
    }
  }

  private String processPageDocument(
      Document document,
      int pageIndex,
      Set<String> processedUrls,
      Set<String> processedNativeIds,
      List<RawPropertyListing> allListings) {
    List<RawPropertyListing> pageListings = rawParser.parseRawListings(document);
    if (pageListings.isEmpty()) {
      return pageIndex == 1 ? "FIRST_PAGE_EMPTY" : "EMPTY_PAGE";
    }
    int addedOnPage = 0;
    for (RawPropertyListing raw : pageListings) {
      if (raw != null && PortalSupport.markProcessedListing(
          processedUrls, processedNativeIds, raw.url(), raw.nativeId())) {
        allListings.add(raw);
        addedOnPage++;
      }
    }
    return addedOnPage == 0 ? "REPEATED_PAGE_CONTENT" : null;
  }

  private record PageFetchOutcome(
      Document document, boolean botProtected, String stopReason, String errorMessage) {}

  public OtthonCentrumUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public OtthonCentrumRawParser getRawParser() {
    return rawParser;
  }
}
