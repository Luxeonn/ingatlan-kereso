package ingatlanok.portal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawExtractionResult;

/**
 * Non-generic Template Method base class for paginated portal extraction strategies.
 *
 * <p>Manages the per-invocation {@link PaginationState}, page-URL cycle guard, first-page failure
 * resolution, bot-protection abort handling, and standardized stop-reason reporting.
 */
public abstract class AbstractPaginatedExtractionStrategy implements ExtractionStrategy {

  public static final int DEFAULT_MAX_PAGES = 20;

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected final int maxPages;

  protected AbstractPaginatedExtractionStrategy(int maxPages, int defaultMaxPages) {
    int fallback = defaultMaxPages > 0 ? defaultMaxPages : DEFAULT_MAX_PAGES;
    this.maxPages = maxPages > 0 ? maxPages : fallback;
  }

  @Override
  public final RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = safeResolveSearchUrl(criteria);
    if (searchUrl == null || searchUrl.isBlank()) {
      return RawExtractionResult.failure(
          method(), getFallbackBaseUrl(), "Keresési feltétel érvénytelen", 400);
    }
    logger.info("{} {} stratégia indítása: {}", getPortalLogName(), method().name(), searchUrl);

    PaginationState state = new PaginationState();
    PaginationStopReason stopReason = PaginationStopReason.MAX_PAGES_REACHED;

    for (int page = 1; page <= maxPages; page++) {
      String pageUrl = buildPageUrl(criteria, searchUrl, page, state);
      if (!state.markVisitedPageUrl(pageUrl)) {
        stopReason = PaginationStopReason.DUPLICATE_PAGE_URL;
        break;
      }

      PageStepOutcome outcome = fetchAndProcessPage(criteria, searchUrl, pageUrl, page, state);
      if (outcome.action() == PageStepOutcome.Action.ABORT_BOT_PROTECTED) {
        return RawExtractionResult.botProtected(method(), searchUrl, outcome.errorMessage());
      }
      if (outcome.action() == PageStepOutcome.Action.ABORT_FIRST_PAGE_FAILURE) {
        return PortalSupport.resolveFirstPageFetchFailure(
            method(), searchUrl, outcome.errorMessage());
      }
      if (outcome.action() == PageStepOutcome.Action.STOP) {
        stopReason =
            outcome.stopReason() != null
                ? outcome.stopReason()
                : PaginationStopReason.MAX_PAGES_REACHED;
        break;
      }
    }

    return buildFinalResult(searchUrl, state, stopReason);
  }

  private String safeResolveSearchUrl(SearchCriteria criteria) {
    try {
      return resolveSearchUrl(criteria);
    } catch (Exception e) {
      logger.error(
          "Hiba a {} keresési URL összeállításakor: {}",
          getPortalLogName(),
          e.getMessage(),
          e);
      return null;
    }
  }

  private RawExtractionResult buildFinalResult(
      String searchUrl, PaginationState state, PaginationStopReason stopReason) {
    logger.info(
        "{} lapozás befejezve: feldolgozott oldalak={}, találatok={}, leállás oka={}",
        getPortalLogName(),
        state.getPagesProcessed(),
        state.getAllListings().size(),
        stopReason.name());

    if (state.isEmpty()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }
    String details =
        String.format("pages=%d, stopReason=%s", state.getPagesProcessed(), stopReason.name());
    return RawExtractionResult.success(state.getAllListings(), method(), details);
  }

  protected abstract String getPortalLogName();

  protected abstract String getFallbackBaseUrl();

  protected abstract String resolveSearchUrl(SearchCriteria criteria);

  protected abstract String buildPageUrl(
      SearchCriteria criteria, String searchUrl, int page, PaginationState state);

  protected abstract PageStepOutcome fetchAndProcessPage(
      SearchCriteria criteria,
      String searchUrl,
      String pageUrl,
      int page,
      PaginationState state);
}
