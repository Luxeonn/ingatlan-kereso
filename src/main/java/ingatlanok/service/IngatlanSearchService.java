package ingatlanok.service;

import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;

public class IngatlanSearchService {

  private static final IngatlanSearchService INSTANCE = new IngatlanSearchService();

  private final SearchExecutionEngine searchEngine;

  private IngatlanSearchService() {
    this(SearchExecutionEngine.getInstance());
  }

  public IngatlanSearchService(SearchExecutionEngine searchEngine) {
    this.searchEngine = searchEngine != null ? searchEngine : SearchExecutionEngine.getInstance();
  }

  public static IngatlanSearchService getInstance() {
    return INSTANCE;
  }

  public SearchResult executeSearch(SearchCriteria criteria) {
    return searchEngine.executeSearch(criteria);
  }
}
