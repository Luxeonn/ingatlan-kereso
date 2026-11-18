package ingatlanok.portal;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;

/**
 * Shared base class for paginated Jsoup {@link Document} extraction strategies (HTML and JSON-LD).
 */
public abstract class AbstractPaginatedHtmlStrategy extends AbstractPaginatedExtractionStrategy {

  protected final PortalUrlBuilder urlBuilder;
  protected final HtmlListingParser rawParser;
  protected final Function<String, Document> documentFetcher;

  protected AbstractPaginatedHtmlStrategy(
      PortalUrlBuilder urlBuilder,
      HtmlListingParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages,
      int defaultMaxPages) {
    super(maxPages, defaultMaxPages);
    this.urlBuilder = Objects.requireNonNull(urlBuilder, "PortalUrlBuilder cannot be null");
    this.rawParser = Objects.requireNonNull(rawParser, "HtmlListingParser cannot be null");
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  protected String resolveSearchUrl(SearchCriteria criteria) {
    return urlBuilder.buildSearchUrl(criteria);
  }

  @Override
  protected String buildPageUrl(
      SearchCriteria criteria, String searchUrl, int page, PaginationState state) {
    return urlBuilder.buildPageUrl(searchUrl, page);
  }

  @Override
  protected PageStepOutcome fetchAndProcessPage(
      SearchCriteria criteria,
      String searchUrl,
      String pageUrl,
      int page,
      PaginationState state) {
    Document document;
    try {
      document = fetchDocumentForPage(pageUrl, page, state);
    } catch (Exception e) {
      logger.error(
          "Hiba a(z) {} {}. oldal lekérésekor: {}", getPortalLogName(), page, e.getMessage(), e);
      return handleFetchFailure(
          page, state, PaginationStopReason.INTERMEDIATE_PAGE_EXCEPTION, e.getMessage());
    }

    if (document == null) {
      logger.warn("Nem sikerült letölteni a(z) {} {}. oldalt: {}", getPortalLogName(), page, pageUrl);
      return handleFetchFailure(
          page,
          state,
          PaginationStopReason.INTERMEDIATE_PAGE_FETCH_FAILED,
          "Nem sikerült letölteni a(z) " + getPortalLogName() + " oldalt");
    }

    if (PortalSupport.isCommonBotChallenge(document)) {
      logger.warn("{}: Bot-védelem detektálva", getPortalLogName());
      if (state.isEmpty()) {
        return PageStepOutcome.abortBotProtected("Bot-védelem detektálva");
      }
      return PageStepOutcome.stop(PaginationStopReason.INTERMEDIATE_PAGE_BOT_PROTECTED);
    }

    state.incrementPagesProcessed();
    onDocumentFetched(document, page, state);
    PaginationStopReason pageStop = processPageDocument(document, page, state);
    return pageStop != null ? PageStepOutcome.stop(pageStop) : PageStepOutcome.continuePagination();
  }

  private PageStepOutcome handleFetchFailure(
      int page, PaginationState state, PaginationStopReason reason, String errorMessage) {
    if (page == 1 && state.isEmpty()) {
      return PageStepOutcome.abortFirstPageFailure(errorMessage);
    }
    return PageStepOutcome.stop(reason);
  }

  protected Document fetchDocumentForPage(String pageUrl, int page, PaginationState state) {
    return documentFetcher.apply(pageUrl);
  }

  protected void onDocumentFetched(Document document, int page, PaginationState state) {
    // Optional hook for subclasses (e.g., extracting chunk filter metadata on page 1).
  }

  protected PaginationStopReason processPageDocument(
      Document document, int page, PaginationState state) {
    List<RawPropertyListing> pageListings = rawParser.parseRawListings(document);
    if (pageListings == null || pageListings.isEmpty()) {
      return page == 1 ? PaginationStopReason.FIRST_PAGE_EMPTY : PaginationStopReason.EMPTY_PAGE;
    }
    int added = state.addNewListings(pageListings);
    if (added == 0) {
      return PaginationStopReason.REPEATED_PAGE_CONTENT;
    }
    if (page < maxPages && hasPaginationWithoutNextPage(document, page, state)) {
      return resolveNoMorePagesReason();
    }
    return null;
  }

  protected PaginationStopReason resolveNoMorePagesReason() {
    return PaginationStopReason.NO_MORE_PAGES;
  }

  protected boolean hasPaginationWithoutNextPage(
      Document document, int page, PaginationState state) {
    return PortalSupport.hasPaginationWithoutNextPage(
        document, getPaginationParamName(), page + 1);
  }

  protected String getPaginationParamName() {
    return "page";
  }

  public PortalUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public HtmlListingParser getRawParser() {
    return rawParser;
  }
}
