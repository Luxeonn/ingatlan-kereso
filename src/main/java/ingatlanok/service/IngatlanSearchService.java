package ingatlanok.service;

import org.springframework.stereotype.Service;

import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;

@Service
public class IngatlanSearchService {

  private final SearchExecutionEngine searchEngine;

  public IngatlanSearchService(SearchExecutionEngine searchEngine) {
    this.searchEngine = java.util.Objects.requireNonNull(searchEngine);
  }

  public SearchResult executeSearch(SearchCriteria criteria) {
    return searchEngine.executeSearch(criteria);
  }
}
