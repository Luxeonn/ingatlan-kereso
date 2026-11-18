package ingatlanok.portal;

/**
 * Outcome of processing a single page inside {@link AbstractPaginatedExtractionStrategy}.
 */
public record PageStepOutcome(
    Action action, PaginationStopReason stopReason, String errorMessage) {

  /** Control-flow directive for the pagination loop. */
  public enum Action {
    CONTINUE,
    STOP,
    ABORT_BOT_PROTECTED,
    ABORT_FIRST_PAGE_FAILURE
  }

  public static PageStepOutcome continuePagination() {
    return new PageStepOutcome(Action.CONTINUE, null, null);
  }

  public static PageStepOutcome stop(PaginationStopReason reason) {
    return new PageStepOutcome(Action.STOP, reason, null);
  }

  public static PageStepOutcome abortBotProtected(String message) {
    return new PageStepOutcome(Action.ABORT_BOT_PROTECTED, PaginationStopReason.BOT_PROTECTED, message);
  }

  public static PageStepOutcome abortFirstPageFailure(String errorMessage) {
    return new PageStepOutcome(
        Action.ABORT_FIRST_PAGE_FAILURE,
        PaginationStopReason.INTERMEDIATE_PAGE_FETCH_FAILED,
        errorMessage);
  }
}
