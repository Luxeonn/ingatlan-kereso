package ingatlanok.portal;

/** Canonical pagination termination reasons emitted in strategy logs and technical details. */
public enum PaginationStopReason {
  MAX_PAGES_REACHED,
  DUPLICATE_PAGE_URL,
  INTERMEDIATE_PAGE_BOT_PROTECTED,
  INTERMEDIATE_PAGE_FETCH_FAILED,
  INTERMEDIATE_PAGE_EXCEPTION,
  BOT_PROTECTED,
  FIRST_PAGE_EMPTY,
  EMPTY_PAGE,
  REPEATED_PAGE_CONTENT,
  NO_MORE_PAGES,
  NO_NEXT_PAGE
}
