package ingatlanok.portal.jofogas;

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

public class JofogasHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(JofogasHtmlStrategy.class);
  private static final int DEFAULT_MAX_PAGES = 2;

  private final JofogasUrlBuilder urlBuilder;
  private final JofogasRawParser rawParser;
  private final Function<String, Document> documentFetcher;
  private final int maxPages;

  public JofogasHtmlStrategy() {
    this(new JofogasUrlBuilder(), new JofogasRawParser(), HttpFetcher::fetchDocument);
  }

  public JofogasHtmlStrategy(
      JofogasUrlBuilder urlBuilder,
      JofogasRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this(urlBuilder, rawParser, documentFetcher, DEFAULT_MAX_PAGES);
  }

  public JofogasHtmlStrategy(
      JofogasUrlBuilder urlBuilder,
      JofogasRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new JofogasUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new JofogasRawParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = resolveSearchUrl(criteria);
    if (searchUrl == null) {
      return RawExtractionResult.failure(
          method(), "https://ingatlan.jofogas.hu", "Keresési feltétel érvénytelen", 400);
    }
    LOGGER.info("Jófogás HTML stratégia indítása: {}", searchUrl);

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
          return RawExtractionResult.failure(method(), searchUrl, fetchOutcome.errorMessage(), 500);
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

    LOGGER.info("Jófogás lapozás befejezve: feldolgozott oldalak={}, találatok={}, leállás oka={}",
        pagesProcessed, allListings.size(), stopReason);

    if (allListings.isEmpty()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }
    String details = String.format("pages=%d, stopReason=%s", pagesProcessed, stopReason);
    return RawExtractionResult.success(allListings, method(), details);
  }

  private String resolveSearchUrl(SearchCriteria criteria) {
    if (criteria == null || criteria.getCity() == null || criteria.getCity().isBlank()) {
      return null;
    }
    try {
      return urlBuilder.buildSearchUrl(criteria);
    } catch (Exception e) {
      LOGGER.error("Hiba a Jófogás keresési URL összeállításakor: {}", e.getMessage(), e);
      return null;
    }
  }

  private PageFetchOutcome fetchPageDocument(String pageUrl, int pageIndex) {
    try {
      Document document = documentFetcher.apply(pageUrl);
      if (document == null) {
        LOGGER.warn("Nem sikerült letölteni a Jófogás {}. oldalt: {}", pageIndex, pageUrl);
        return new PageFetchOutcome(
            null, false, "INTERMEDIATE_PAGE_FETCH_FAILED", "Nem sikerült letölteni a Jófogás oldalt");
      }
      if (PortalSupport.isCommonBotChallenge(document)) {
        LOGGER.warn("Jófogás: Bot-védelem detektálva");
        return new PageFetchOutcome(null, true, "BOT_PROTECTED", "Bot-védelem detektálva");
      }
      return new PageFetchOutcome(document, false, null, null);
    } catch (Exception e) {
      LOGGER.error("Hiba a Jófogás {}. oldal lekérésekor: {}", pageIndex, e.getMessage(), e);
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
    int addedOnPage = collectPageListings(
        pageListings, processedUrls, processedNativeIds, allListings);
    return addedOnPage == 0 ? "REPEATED_PAGE_CONTENT" : null;
  }

  private int collectPageListings(
      List<RawPropertyListing> pageListings,
      Set<String> processedUrls,
      Set<String> processedNativeIds,
      List<RawPropertyListing> allListings) {
    int added = 0;
    for (RawPropertyListing raw : pageListings) {
      if (raw != null && PortalSupport.markProcessedListing(
          processedUrls, processedNativeIds, raw.url(), raw.nativeId())) {
        allListings.add(raw);
        added++;
      }
    }
    return added;
  }

  private record PageFetchOutcome(
      Document document, boolean botProtected, String stopReason, String errorMessage) {}

  public JofogasUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public JofogasRawParser getRawParser() {
    return rawParser;
  }
}
