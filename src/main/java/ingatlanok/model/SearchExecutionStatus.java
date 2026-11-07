package ingatlanok.model;

/**
 * Overall search execution status distinguishing between full success,
 * partial success (some portals failed or bot-protected), and total failure.
 */
public enum SearchExecutionStatus {
  SUCCESS,
  PARTIAL_SUCCESS,
  TOTAL_FAILURE
}
