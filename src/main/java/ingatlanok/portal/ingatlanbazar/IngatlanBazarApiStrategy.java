package ingatlanok.portal.ingatlanbazar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalSupport;

public class IngatlanBazarApiStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarApiStrategy.class);
  private static final int DEFAULT_MAX_PAGES = 2;

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final IngatlanBazarRawParser rawParser;
  private final Function<String, String> jsonFetcher;
  private final int maxPages;

  public IngatlanBazarApiStrategy() {
    this(new IngatlanBazarQueryBuilder(), new IngatlanBazarRawParser(), HttpFetcher::fetchJson);
  }

  public IngatlanBazarApiStrategy(
      IngatlanBazarQueryBuilder queryBuilder,
      IngatlanBazarRawParser rawParser,
      Function<String, String> jsonFetcher) {
    this(queryBuilder, rawParser, jsonFetcher, DEFAULT_MAX_PAGES);
  }

  public IngatlanBazarApiStrategy(
      IngatlanBazarQueryBuilder queryBuilder,
      IngatlanBazarRawParser rawParser,
      Function<String, String> jsonFetcher,
      int maxPages) {
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.rawParser = rawParser != null ? rawParser : new IngatlanBazarRawParser();
    this.jsonFetcher = jsonFetcher != null ? jsonFetcher : HttpFetcher::fetchJson;
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.API;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String webSearchUrl = queryBuilder.buildSearchUrl(criteria);
    LOGGER.info("Ingatlanbazár API lekérdezés indítása: {}", webSearchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    Set<String> processedNativeIds = new HashSet<>();
    Set<String> visitedPageUrls = new HashSet<>();
    int pagesProcessed = 0;
    String stopReason = "MAX_PAGES_REACHED";

    for (int page = 1; page <= maxPages; page++) {
      String apiUrl = queryBuilder.buildApiSearchUrl(criteria, page);
      if (apiUrl == null || !visitedPageUrls.add(apiUrl)) {
        stopReason = "DUPLICATE_PAGE_URL";
        break;
      }

      PageFetchOutcome fetchOutcome = fetchPageJson(apiUrl, page);
      if (fetchOutcome.json() == null) {
        if (page == 1 && allListings.isEmpty()) {
          return RawExtractionResult.failure(
              method(), webSearchUrl, fetchOutcome.errorMessage(), 500);
        }
        stopReason = fetchOutcome.stopReason();
        break;
      }

      pagesProcessed++;
      String pageStopReason = processPageJson(
          fetchOutcome.json(), page, processedUrls, processedNativeIds, allListings);
      if (pageStopReason != null) {
        stopReason = pageStopReason;
        break;
      }
    }

    LOGGER.info("Ingatlanbazár lapozás befejezve: feldolgozott oldalak={}, találatok={}, leállás oka={}",
        pagesProcessed, allListings.size(), stopReason);

    if (allListings.isEmpty()) {
      return RawExtractionResult.empty(method(), webSearchUrl);
    }
    String details = String.format("pages=%d, stopReason=%s", pagesProcessed, stopReason);
    return RawExtractionResult.success(allListings, method(), details);
  }

  private PageFetchOutcome fetchPageJson(String apiUrl, int page) {
    try {
      String json = jsonFetcher.apply(apiUrl);
      if (json == null || json.isBlank()) {
        LOGGER.warn("Nem sikerült letölteni az Ingatlanbazár {}. oldal JSON választ: {}", page, apiUrl);
        return new PageFetchOutcome(
            null,
            "INTERMEDIATE_PAGE_FETCH_FAILED",
            "Nem sikerült elérni az Ingatlanbazár REST API-t");
      }
      return new PageFetchOutcome(json, null, null);
    } catch (Exception e) {
      LOGGER.error("Hiba az Ingatlanbazár {}. oldal API lekérésekor: {}", page, e.getMessage(), e);
      return new PageFetchOutcome(null, "INTERMEDIATE_PAGE_EXCEPTION", e.getMessage());
    }
  }

  private String processPageJson(
      String json,
      int page,
      Set<String> processedUrls,
      Set<String> processedNativeIds,
      List<RawPropertyListing> allListings) {
    List<RawPropertyListing> pageListings = rawParser.parseRawListings(json);
    if (pageListings.isEmpty()) {
      return page == 1 ? "FIRST_PAGE_EMPTY" : "EMPTY_PAGE";
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

  private record PageFetchOutcome(String json, String stopReason, String errorMessage) {}

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  public IngatlanBazarRawParser getRawParser() {
    return rawParser;
  }
}
