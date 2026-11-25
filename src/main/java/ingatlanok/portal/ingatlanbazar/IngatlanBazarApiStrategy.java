package ingatlanok.portal.ingatlanbazar;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.AbstractPaginatedExtractionStrategy;
import ingatlanok.portal.PageStepOutcome;
import ingatlanok.portal.PaginationState;
import ingatlanok.portal.PaginationStopReason;

public class IngatlanBazarApiStrategy extends AbstractPaginatedExtractionStrategy {

  public static final int DEFAULT_MAX_PAGES = AbstractPaginatedExtractionStrategy.DEFAULT_MAX_PAGES;

  private static final Logger LOGGER = LoggerFactory.getLogger(IngatlanBazarApiStrategy.class);

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final IngatlanBazarRawParser rawParser;
  private final Function<String, String> jsonFetcher;

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
    super(maxPages, DEFAULT_MAX_PAGES);
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.rawParser = rawParser != null ? rawParser : new IngatlanBazarRawParser();
    this.jsonFetcher = jsonFetcher != null ? jsonFetcher : HttpFetcher::fetchJson;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.API;
  }

  @Override
  protected String getPortalLogName() {
    return "Ingatlanbazár";
  }

  @Override
  protected String getFallbackBaseUrl() {
    return IngatlanBazarQueryBuilder.BASE_URL;
  }

  @Override
  protected String resolveSearchUrl(SearchCriteria criteria) {
    return queryBuilder.buildSearchUrl(criteria);
  }

  @Override
  protected String buildPageUrl(
      SearchCriteria criteria, String searchUrl, int page, PaginationState state) {
    return queryBuilder.buildApiSearchUrl(criteria, page);
  }

  @Override
  protected PageStepOutcome fetchAndProcessPage(
      SearchCriteria criteria,
      String searchUrl,
      String pageUrl,
      int page,
      PaginationState state) {
    String json;
    try {
      json = jsonFetcher.apply(pageUrl);
    } catch (Exception e) {
      LOGGER.error("Hiba az Ingatlanbazár {}. oldal API lekérésekor: {}", page, e.getMessage(), e);
      return page == 1 && state.isEmpty()
          ? PageStepOutcome.abortFirstPageFailure(e.getMessage())
          : PageStepOutcome.stop(PaginationStopReason.INTERMEDIATE_PAGE_EXCEPTION);
    }

    if (json == null || json.isBlank()) {
      LOGGER.warn("Nem sikerült letölteni az Ingatlanbazár {}. oldal JSON választ: {}", page, pageUrl);
      return page == 1 && state.isEmpty()
          ? PageStepOutcome.abortFirstPageFailure("Nem sikerült elérni az Ingatlanbazár REST API-t")
          : PageStepOutcome.stop(PaginationStopReason.INTERMEDIATE_PAGE_FETCH_FAILED);
    }

    state.incrementPagesProcessed();
    List<RawPropertyListing> pageListings = rawParser.parseRawListings(json);
    if (pageListings.isEmpty()) {
      return PageStepOutcome.stop(
          page == 1 ? PaginationStopReason.FIRST_PAGE_EMPTY : PaginationStopReason.EMPTY_PAGE);
    }
    int added = state.addNewListings(pageListings);
    return added == 0
        ? PageStepOutcome.stop(PaginationStopReason.REPEATED_PAGE_CONTENT)
        : PageStepOutcome.continuePagination();
  }

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  public IngatlanBazarRawParser getRawParser() {
    return rawParser;
  }
}
